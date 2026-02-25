package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteEvent;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.ScheduleCreatedEvent;
import com.kiero.schedule.application.dto.ScheduleUpdateRequest;
import com.kiero.schedule.application.dto.TodayScheduleResponse;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleCommandUseCase;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.ScheduleEventPort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.application.service.resolver.ScheduleUpdateCase;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatus;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatusResolver;

import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
	private final SchedulePersistencePort schedulePersistencePort;
	private final ScheduleRepeatDaysPersistencePort scheduleRepeatDaysPersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;

	@Override
	@Transactional
	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ScheduleErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		boolean isEffectsToChildSchedule = false;

		validateAddAndUpdateRequest(request.isRecurring(), request.dayOfWeek(), request.dates());
		throwExceptionWhenScheduleDuplicated(request.isRecurring(), request.dayOfWeek(), request.startTime(), request.endTime(), request.dates(), child.getId());

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
			if (dayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now)) {
				recalculateTodayStoneTypes(childId);
				isEffectsToChildSchedule = true;
			}

		} else {
			List<LocalDate> dates = dateParser(request.dates());
			List<ScheduleDetail> details = dates.stream()
				.distinct()
				.sorted()
				.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
				.toList();
			detailPort.saveAll(details);

			// 추가된 일정이 오늘 일정이고, 추가된 일정의 시작 시간이 현재 시간 이후라면 불조각 종류를 재계산함
			if (dates.contains(today) && request.startTime().isAfter(now)) {
				recalculateTodayStoneTypes(childId);
				isEffectsToChildSchedule = true;
			}
		}

		// 아이의 오늘 일정에 영향이 있을 때만 이벤트 전송
		if (isEffectsToChildSchedule) eventPort.publish(new ScheduleCreatedEvent(childId, saved.getName()));
	}

	@Override
	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now(clock);

		List<ScheduleDetail> allScheduleDetails = detailPort.findByDateAndChildId(today, childId).stream()
			.sorted(Comparator
					.comparing((ScheduleDetail sd) -> sd.getSchedule().getStartTime())
					.thenComparing(sd -> sd.getSchedule().getId()))
			.toList();

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
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

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
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

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

	@Override
	@Transactional
	public void updateSchedule(Long parentId, Long scheduleId, LocalDate selectedDate, ScheduleUpdateRequest request) {

		validateAddAndUpdateRequest(request.isRecurring(), request.dayOfWeek(), request.dates());

		Schedule schedule = schedulePersistencePort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!parentId.equals(schedule.getParent().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		throwExceptionWhenScheduleDuplicated(request.isRecurring(), request.dayOfWeek(), request.startTime(), request.endTime(), request.dates(), schedule.getChild().getId());

		boolean isEffectsToChildSchedule = false;

		ScheduleUpdateCase scheduleUpdateCase = scheduleUpdateCaseResolver(schedule, request);

		log.info("scheduleUpdateCase: " + scheduleUpdateCase);

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Long childId = schedule.getChild().getId();

		switch (scheduleUpdateCase) {

			/*
			단일일정 -> 단일일정일 때,
			1) 기존의 scheduleDetail을 삭제합니다.
			2) 새로운 schedule을 생성합니다.
			3) scheduleDetail을 생성합니다.

			새로 생성된 일정의 날짜가 오늘이라면,
			4) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			5) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case NormalToNormal -> {
				Schedule saved = deleteOriginalScheduleDetailAndSaveNewSchedule(schedule, selectedDate, request, false);

				List<LocalDate> dates = dateParser(request.dates());
				List<ScheduleDetail> details = dates.stream()
					.distinct()
					.sorted()
					.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
					.toList();
				detailPort.saveAll(details);

				if (dates.contains(today) && request.startTime().isAfter(now)) {
					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			/*
			단일일정 -> 반복일정일 때,
			1) 기존의 scheduleDetail을 삭제합니다.
			2) 새로운 schedule을 생성합니다.
			3) 새로운 scheduleRepeatDays를 생성합니다.

			새로 생성된 일정의 요일에 오늘이 해당한다면,
			4) scheduleDetail을 추가로 생성합니다.
			5) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			6) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case NormalToRecurring -> {
				Schedule saved = deleteOriginalScheduleDetailAndSaveNewSchedule(schedule, selectedDate, request, true);

				List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
				List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
					.map(day -> ScheduleRepeatDays.create(day, saved))
					.toList();
				repeatDaysPort.saveAll(repeatDays);

				createScheduleDetailOfTodayRecurringSchedules(today);

				DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());
				if (dayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now)) {
					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			/*
			반복일정 -> 반복일정이고 요일 변화가 있을 때,
			1) 기존의 scheduleDetail을 삭제합니다.
			2) 기존 반복요일과 요청 반복요일을 비교해 삭제해야 할 요일, 추가해야 할 요일을 추출합니다.
			3) 2)에 맞춰 삭제 혹은 추가 작업을 진행합니다.

			새로 생성된 일정의 요일에 오늘이 해당한다면,
			4) scheduleDetail을 추가로 생성합니다.
			5) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			6) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case RecurringToRecurring -> {
				// 수정하고자 하는 일정의 scheduleDetail 삭제
				// 기획 답변 듣고 수정해야 할 필요 O
				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(schedule.getId(), selectedDate);

				List<DayOfWeek> originalDayOfWeeks =
					scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(scheduleId);
				List<DayOfWeek> requestDayOfWeeks = dayOfWeekParser(request.dayOfWeek());

				EnumSet<DayOfWeek> originalSet = originalDayOfWeeks.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(originalDayOfWeeks);
				EnumSet<DayOfWeek> requestSet = requestDayOfWeeks.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(requestDayOfWeeks);

				EnumSet<DayOfWeek> toAddSet = EnumSet.copyOf(requestSet);
				toAddSet.removeAll(originalSet);

				EnumSet<DayOfWeek> toBeDeletedSet = EnumSet.copyOf(originalSet);
				toBeDeletedSet.removeAll(requestSet);

				List<DayOfWeek> toAdd = new ArrayList<>(toAddSet);
				List<DayOfWeek> toBeDeleted = new ArrayList<>(toBeDeletedSet);

				List<ScheduleRepeatDays> toAddSD = toAdd.stream()
					.map(d -> ScheduleRepeatDays.create(d, schedule))
					.toList();

				scheduleRepeatDaysPersistencePort.saveAll(toAddSD);
				scheduleRepeatDaysPersistencePort.deleteByScheduleIdAndDayOfWeekIn(scheduleId, toBeDeleted);

				createScheduleDetailOfTodayRecurringSchedules(today);

				DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());
				if (requestDayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now)) {
					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			case RecurringToRecurringIncludeFollowing -> {

			}

		}
	}

	private void validateAddAndUpdateRequest(boolean isRecurring, String dayOfWeek, String dates) {
		if (isRecurring && (dayOfWeek == null || dayOfWeek.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}
		if (!isRecurring && (dates == null || dates.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
		}
		if (dayOfWeek != null && dates != null) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}
	}

	private void throwExceptionWhenScheduleDuplicated(boolean isRecurring, String dayOfWeek, LocalTime startTime, LocalTime endTime, String requestDates, Long childId) {
		if (isRecurring) {
			List<DayOfWeek> targetDays = dayOfWeekParser(dayOfWeek);
			List<Schedule> existingRecurring = repeatDaysPort.findSchedulesByChildIdAndDayOfWeeks(childId, targetDays);

			boolean conflictWithRecurring = existingRecurring.stream()
				.anyMatch(s -> isTimeOverlapped(startTime, endTime, s.getStartTime(), s.getEndTime()));

			if (conflictWithRecurring) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

			LocalDate today = LocalDate.now(clock);
			List<ScheduleDetail> normalsFromToday = detailPort.findAllByScheduleChildIdAndDateGreaterThanEqual(childId, today);

			boolean conflictWithNormal = normalsFromToday.stream()
				.filter(sd -> targetDays.contains(DayOfWeek.from(sd.getDate().getDayOfWeek())))
				.anyMatch(sd -> isTimeOverlapped(
					startTime, endTime,
					sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
				));

			if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
			return;
		}

		List<LocalDate> dates = dateParser(requestDates);
		List<ScheduleDetail> thatDayDetails = detailPort.findByDateInAndChildId(dates, childId);

		boolean conflictWithNormal = thatDayDetails.stream()
			.anyMatch(sd -> isTimeOverlapped(
				startTime, endTime,
				sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
			));

		if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

		List<DayOfWeek> targetDays = dates.stream()
			.map(date -> DayOfWeek.from(date.getDayOfWeek()))
			.distinct()
			.toList();

		List<Schedule> existingRecurringOnThatDay = repeatDaysPort.findSchedulesByChildIdAndDayOfWeekIn(childId, targetDays);

		boolean conflictWithRecurring = existingRecurringOnThatDay.stream()
			.anyMatch(s -> isTimeOverlapped(startTime, endTime, s.getStartTime(), s.getEndTime()));

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

	private ScheduleUpdateCase scheduleUpdateCaseResolver(Schedule schedule, ScheduleUpdateRequest request) {

		if (!schedule.isRecurring()) {
			if (!request.isRecurring()) return ScheduleUpdateCase.NormalToNormal;
			else return ScheduleUpdateCase.NormalToRecurring;
		} else {
			if (!request.isRecurring()) return ScheduleUpdateCase.RecurringToNormal;
			else {
				List<DayOfWeek> originalRepeatDays = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(schedule.getId());
				List<DayOfWeek> requestRepeatDays = dayOfWeekParser(request.dayOfWeek());
				boolean isSame = new HashSet<>(originalRepeatDays).equals(new HashSet<>(requestRepeatDays));

				if (!isSame) return ScheduleUpdateCase.RecurringToRecurring;
				else if (request.includeFollowing()) return ScheduleUpdateCase.RecurringToRecurringIncludeFollowing;
				else return ScheduleUpdateCase.RecurringToRecurringExceptFollowing;
			}
		}
	}

	private Schedule deleteOriginalScheduleDetailAndSaveNewSchedule(Schedule schedule, LocalDate selectedDate, ScheduleUpdateRequest request, boolean isRecurring) {
		// 수정하고자 하는 일정의 scheduleDetail 삭제
		scheduleDetailPersistencePort.deleteByScheduleIdAndDate(schedule.getId(), selectedDate);

		// 새로운 schedule 생성 및 저장
		Schedule newSchedule = Schedule.create(
			schedule.getParent(),
			schedule.getChild(),
			request.name(),
			request.startTime(),
			request.endTime(),
			request.scheduleColor(),
			isRecurring
		);
		return schedulePersistencePort.save(newSchedule);
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
}