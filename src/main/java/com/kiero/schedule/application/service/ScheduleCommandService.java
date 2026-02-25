package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ScheduleErrorCode.NOT_ALLOWED_TO_CHILD);
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
		} else {
			List<LocalDate> dates = dateParser(request.dates());
			List<ScheduleDetail> details = dates.stream()
				.distinct()
				.sorted()
				.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
				.toList();
			detailPort.saveAll(details);
		}

		eventPort.publish(new ScheduleCreatedEvent(childId, saved.getName()));
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

		List<ScheduleDetail> filteredAll = filterTodayCreatedSchedules(today, all, null);

		int totalSchedule = (int) filteredAll.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		List<StoneType> gotStones = filteredAll.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.map(ScheduleDetail::getStoneType)
			.toList();

		LocalDateTime now = LocalDateTime.now(clock);
		filteredAll.forEach(sd -> sd.changeStoneUsedAt(now));

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
}