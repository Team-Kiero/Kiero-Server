package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteEvent;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.ScheduleCreatedEvent;
import com.kiero.schedule.application.dto.TodayScheduleResponse;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleCommandUseCase;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.ScheduleEventPort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;
import com.kiero.schedule.domain.policy.TodayScheduleStatusResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleCommandService implements ScheduleCommandUseCase {

	private static final int ALL_SCHEDULE_SUCCESS_REWARD = 10;

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	private final SchedulePersistencePort schedulePort;
	private final ScheduleRepeatDaysPersistencePort repeatDaysPort;
	private final ScheduleDetailPersistencePort detailPort;

	private final ScheduleEventPort eventPort;
	private final Clock clock;

	@Override
	@Transactional
	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		LocalDate today = LocalDate.now(clock);

		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		validateAddRequest(request);
		throwExceptionWhenScheduleDuplicated(request, child.getId());

		Schedule schedule = Schedule.create(
			parent, child,
			request.name(),
			request.startTime(),
			request.endTime(),
			request.scheduleColor(),
			request.isRecurring()
		);

		Schedule saved = schedulePort.save(schedule);

		if (request.isRecurring()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, saved))
				.toList();
			repeatDaysPort.saveAll(repeatDays);

			// 당일 생성된 반복 일정 중 오늘 요일이면 scheduleDetail 생성
			createScheduleDetailOfTodayRecurringSchedules(today);


			// 추가된 일정의 요일에 오늘이 포함된다면 오늘 일정들의 stoneType 재계산
			DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());
			if (dayOfWeeks.contains(todayDayOfWeek)) recalculateTodayStoneTypes(childId);

		} else {
			List<LocalDate> dates = dateParser(request.dates());
			List<ScheduleDetail> details = dates.stream()
				.distinct()
				.sorted()
				.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
				.toList();
			detailPort.saveAll(details);

			// 추가된 일정의 날짜가 오늘이라면 오늘 일정들의 stoneType 재계산
			if (dates.contains(today)) recalculateTodayStoneTypes(childId);
		}

		eventPort.publish(new ScheduleCreatedEvent(childId, saved.getName()));
	}

	@Override
	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now(clock);

		List<ScheduleDetail> allScheduleDetails = detailPort.findByDateAndChildId(today, childId);

		List<ScheduleDetail> pendingAndVerified = allScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING || sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.toList();

		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);

		List<ScheduleDetail> filteredPendingAndVerified = filterTodayCreatedSchedules(today, pendingAndVerified, earliestStoneUsedAt);
		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails, earliestStoneUsedAt);

		markPassedPendingSchedulesAsFailed(filteredPendingAndVerified);
		markPassedVerifiedSchedulesAsCompleted(filteredPendingAndVerified);

		List<ScheduleDetail> todo2 = findTodoScheduleAndNextTodoSchedule(filteredPendingAndVerified);
		ScheduleDetail todo = todo2.size() > 0 ? todo2.get(0) : null;
		ScheduleDetail nextTodo = todo2.size() > 1 ? todo2.get(1) : null;

		int totalSchedule = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		int earnedStones = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.count();

		boolean isSkippable = nextTodo != null;

		TodayScheduleStatus todayScheduleStatus = TodayScheduleStatusResolver.resolve(
			earnedStones,
			todo,
			filteredAllScheduleDetails,
			earliestStoneUsedAt
		);

		if (todo == null) {
			return TodayScheduleResponse.of(
				null, 0, null, null, null, null,
				totalSchedule, earnedStones,
				todayScheduleStatus,
				isSkippable,
				false
			);
		}

		int order = filteredAllScheduleDetails.indexOf(todo) + 1;
		boolean isNowScheduleVerified = todo.getScheduleStatus() == ScheduleStatus.VERIFIED;

		return TodayScheduleResponse.of(
			todo.getId(),
			order,
			todo.getSchedule().getStartTime(),
			todo.getSchedule().getEndTime(),
			todo.getSchedule().getName(),
			todo.getStoneType(),
			totalSchedule,
			earnedStones,
			todayScheduleStatus,
			isSkippable,
			isNowScheduleVerified
		);
	}


	@Override
	@Transactional
	public void skipNowSchedule(Long childId, Long scheduleDetailId) {
		childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		ScheduleDetail scheduleDetail = detailPort.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (scheduleDetail.getScheduleStatus() == ScheduleStatus.PENDING) {
			scheduleDetail.changeScheduleStatus(ScheduleStatus.SKIPPED);
		} else if (scheduleDetail.getScheduleStatus() == ScheduleStatus.VERIFIED) {
			scheduleDetail.changeScheduleStatus(ScheduleStatus.COMPLETED);
		} else {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_COULD_NOT_BE_SKIPPED);
		}
	}

	@Override
	@Transactional
	public void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request) {
		ScheduleDetail scheduleDetail = detailPort.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (scheduleDetail.getScheduleStatus() == ScheduleStatus.VERIFIED
			|| scheduleDetail.getScheduleStatus() == ScheduleStatus.COMPLETED) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ALREADY_COMPLETED);
		}

		if (scheduleDetail.getStoneUsedAt() != null) {
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);
		}

		scheduleDetail.changeScheduleStatus(ScheduleStatus.VERIFIED);
		scheduleDetail.changeImageUrl(request.imageUrl());

		eventPort.publish(new NowScheduleCompleteEvent(
			scheduleDetail.getSchedule().getChild().getId(),
			scheduleDetail.getSchedule().getName(),
			scheduleDetail.getImageUrl(),
			LocalDateTime.now(clock)
		));
	}

	@Override
	@Transactional
	public FireLitResponse fireLit(Long childId) {
		LocalDate today = LocalDate.now(clock);

		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		List<ScheduleDetail> all = detailPort.findByDateAndChildId(today, childId);

		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(all);
		if (earliestStoneUsedAt != null) {
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);
		}

		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, all, null);

		int totalSchedule = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		List<StoneType> gotStones = filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.map(ScheduleDetail::getStoneType)
			.toList();

		LocalDateTime now = LocalDateTime.now(clock);
		filteredAllScheduleDetails.forEach(sd -> sd.changeStoneUsedAt(now));

		int gotStonesCount = gotStones.size();
		int earnedCoinAmount = 0;

		if (totalSchedule == gotStonesCount && totalSchedule != 0) {
			child.addCoin(ALL_SCHEDULE_SUCCESS_REWARD);
			earnedCoinAmount = ALL_SCHEDULE_SUCCESS_REWARD;
		}

		eventPort.publish(new FireLitEvent(child.getId(), earnedCoinAmount, LocalDateTime.now(clock)));
		return FireLitResponse.of(gotStones, earnedCoinAmount);
	}

	@Override
	@Transactional
	public void createTodayScheduleDetail() {
		LocalDate today = LocalDate.now(clock);
		DayOfWeek customDayOfWeek = DayOfWeek.valueOf(today.getDayOfWeek().name().substring(0, 3));

		List<Schedule> schedules = repeatDaysPort.findSchedulesToCreateTodayDetail(customDayOfWeek, today);
		List<ScheduleDetail> scheduleDetails = schedules.stream()
			.map(schedule -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, schedule))
			.toList();

		detailPort.saveAll(scheduleDetails);

		List<ScheduleDetail> allScheduleDetails = detailPort.findAllByDate(today);

		calculateStoneTypePerScheduleDetail(allScheduleDetails);
	}

	private void calculateStoneTypePerScheduleDetail(List<ScheduleDetail> scheduleDetails) {

		if (scheduleDetails.isEmpty()) return;

		Map<Long, List<ScheduleDetail>> byChild = scheduleDetails.stream()
			.collect(Collectors.groupingBy(sd -> sd.getSchedule().getChild().getId()));


		for (List<ScheduleDetail> childDetails : byChild.values()) {
			childDetails.sort(
				Comparator
					.comparing((ScheduleDetail sd) -> sd.getSchedule().getStartTime())
					.thenComparing(sd -> sd.getSchedule().getId())
			);

			for (int i = 0; i < childDetails.size(); i++) {
				ScheduleDetail sd = childDetails.get(i);

				switch (i % 3) {
					case 0 -> sd.changeStoneType(StoneType.COURAGE);
					case 1 -> sd.changeStoneType(StoneType.GRIT);
					case 2 -> sd.changeStoneType(StoneType.WISDOM);
				}
			}
		}
	}

	private void recalculateTodayStoneTypes(Long childId) {
		LocalDate today = LocalDate.now(clock);

		List<ScheduleDetail> allScheduleDetails = detailPort.findByDateAndChildId(today, childId);
		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);

		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails, earliestStoneUsedAt);

		calculateStoneTypePerScheduleDetail(filteredAllScheduleDetails);
	}

	private void validateAddRequest(ScheduleAddRequest request) {
		if (request.isRecurring() && (request.dayOfWeek() == null || request.dayOfWeek().isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}
		if (!request.isRecurring() && (request.dates() == null || request.dates().isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
		}
		if (request.dayOfWeek() != null && request.dates() != null) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}
	}

	private void throwExceptionWhenScheduleDuplicated(ScheduleAddRequest request, Long childId) {
		if (request.isRecurring()) {
			List<DayOfWeek> targetDays = dayOfWeekParser(request.dayOfWeek());
			List<Schedule> existingRecurring = repeatDaysPort.findSchedulesByChildIdAndDayOfWeeks(childId, targetDays);

			boolean conflictWithRecurring = existingRecurring.stream()
				.anyMatch(s -> isTimeOverlapped(request.startTime(), request.endTime(), s.getStartTime(), s.getEndTime()));

			if (conflictWithRecurring) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

			LocalDate today = LocalDate.now(clock);
			List<ScheduleDetail> normalsFromToday = detailPort.findAllByScheduleChildIdAndDateGreaterThanEqual(childId, today);

			boolean conflictWithNormal = normalsFromToday.stream()
				.filter(sd -> targetDays.contains(DayOfWeek.from(sd.getDate().getDayOfWeek())))
				.anyMatch(sd -> isTimeOverlapped(
					request.startTime(), request.endTime(),
					sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
				));

			if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
			return;
		}

		List<LocalDate> dates = dateParser(request.dates());
		List<ScheduleDetail> thatDayDetails = detailPort.findByDateInAndChildId(dates, childId);

		boolean conflictWithNormal = thatDayDetails.stream()
			.anyMatch(sd -> isTimeOverlapped(
				request.startTime(), request.endTime(),
				sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
			));

		if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

		List<DayOfWeek> targetDays = dates.stream()
			.map(date -> DayOfWeek.from(date.getDayOfWeek()))
			.distinct()
			.toList();

		List<Schedule> existingRecurringOnThatDay = repeatDaysPort.findSchedulesByChildIdAndDayOfWeekIn(childId, targetDays);

		boolean conflictWithRecurring = existingRecurringOnThatDay.stream()
			.anyMatch(s -> isTimeOverlapped(request.startTime(), request.endTime(), s.getStartTime(), s.getEndTime()));

		if (conflictWithRecurring) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
	}

	private boolean isTimeOverlapped(LocalTime newStart, LocalTime newEnd, LocalTime oldStart, LocalTime oldEnd) {
		return newStart.isBefore(oldEnd) && newEnd.isAfter(oldStart);
	}

	private List<LocalDate> dateParser(String dates) {
		try {
			return Stream.of(dates.split(","))
				.map(String::trim)
				.map(LocalDate::parse)
				.distinct()
				.sorted()
				.toList();
		} catch (DateTimeParseException e) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_FORMAT);
		}
	}

	private List<DayOfWeek> dayOfWeekParser(String dayOfWeek) {
		try {
			return Stream.of(dayOfWeek.split(","))
				.map(String::trim)
				.map(String::toUpperCase)
				.map(DayOfWeek::valueOf)
				.toList();
		} catch (IllegalArgumentException e) {
			throw new KieroException(ScheduleErrorCode.INVALID_DAY_OF_WEEK);
		}
	}

	// 당일 생성된 일정 중, startTime과 stone 사용 여부로 유효한 일정만 필터링하는 private method
	private List<ScheduleDetail> filterTodayCreatedSchedules(LocalDate today, List<ScheduleDetail> scheduleDetails, LocalDateTime earliestStoneUsedAt) {
		return scheduleDetails.stream()
			.filter(sd -> {
				Schedule schedule = sd.getSchedule();
				LocalDateTime createdAt = schedule.getCreatedAt();

				if (!createdAt.toLocalDate().equals(today)) return true;
				if (createdAt.toLocalTime().isAfter(schedule.getStartTime())) return false;

				return earliestStoneUsedAt == null || !createdAt.isAfter(earliestStoneUsedAt);
			})
			.toList();
	}

	private LocalDateTime findEarliestStoneUsedAt(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.map(ScheduleDetail::getStoneUsedAt)
			.filter(Objects::nonNull)
			.min(LocalDateTime::compareTo)
			.orElse(null);
	}

	private void markPassedPendingSchedulesAsFailed(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now(clock);
		scheduleDetails.stream()
			.filter(sd -> sd.getSchedule().getEndTime().isBefore(now) && sd.getScheduleStatus() == ScheduleStatus.PENDING)
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.FAILED));
	}

	private void markPassedVerifiedSchedulesAsCompleted(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now(clock);
		scheduleDetails.stream()
			.filter(sd -> sd.getSchedule().getEndTime().isBefore(now) && sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.COMPLETED));
	}

	private List<ScheduleDetail> findTodoScheduleAndNextTodoSchedule(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING || sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.limit(2)
			.toList();
	}

	// 오늘이 반복 요일에 해당하고, 오늘 생성되었으며, 아직 오늘자 ScheduleDetail이 생성되지 않은 반복일정들의 오늘자 ScheduleDetail을 생성하는 private method
	private void createScheduleDetailOfTodayRecurringSchedules(LocalDate today) {
		LocalDateTime startOfToday = today.atStartOfDay();
		DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

		List<Schedule> schedules = schedulePort.findRecurringSchedulesToGenerateTodayDetail(
			startOfToday,
			todayDayOfWeek,
			today
		);

		if (schedules.isEmpty()) return;

		List<ScheduleDetail> details = schedules.stream()
			.map(schedule -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, schedule))
			.toList();

		detailPort.saveAll(details);
	}
}