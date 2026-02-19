package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import com.kiero.schedule.application.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.application.dto.NormalScheduleDto;
import com.kiero.schedule.application.dto.RecurringScheduleDto;
import com.kiero.schedule.application.dto.ScheduleTabResponse;
import com.kiero.schedule.application.dto.TodayScheduleResponse;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleColor;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;
import com.kiero.schedule.domain.policy.TodayScheduleStatusResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService implements ScheduleQueryUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	private final SchedulePersistencePort schedulePort;
	private final ScheduleRepeatDaysPersistencePort repeatDaysPort;
	private final ScheduleDetailPersistencePort detailPort;

	private final Clock clock;

	@Override
	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now(clock);

		// 당일 생성된 반복 일정 중 오늘 요일이면 scheduleDetail 생성(수동)
		createScheduleDetailOfTodayRecurringSchedules(today);

		List<ScheduleDetail> all = detailPort.findByDateAndChildId(today, childId);

		List<ScheduleDetail> pendingAndVerified = all.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING || sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.toList();

		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(all);

		List<ScheduleDetail> filteredPendingAndVerified = filterTodayCreatedSchedules(today, pendingAndVerified, earliestStoneUsedAt);
		List<ScheduleDetail> filteredAll = filterTodayCreatedSchedules(today, all, earliestStoneUsedAt);

		markPassedPendingSchedulesAsFailed(filteredPendingAndVerified);
		markPassedVerifiedSchedulesAsCompleted(filteredPendingAndVerified);

		List<ScheduleDetail> todo2 = findTodoScheduleAndNextTodoSchedule(filteredPendingAndVerified);
		ScheduleDetail todo = todo2.size() > 0 ? todo2.get(0) : null;
		ScheduleDetail nextTodo = todo2.size() > 1 ? todo2.get(1) : null;

		stoneTypeCalculateAndSetter(filteredAll, todo);

		int totalSchedule = (int) filteredAll.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		int earnedStones = (int) filteredAll.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.count();

		boolean isSkippable = nextTodo != null;

		TodayScheduleStatus todayScheduleStatus = TodayScheduleStatusResolver.resolve(
			earnedStones,
			todo,
			filteredAll,
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

		int order = filteredAll.indexOf(todo) + 1;
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
	public DefaultScheduleContentResponse getDefaultSchedule(Long parentId, Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		ScheduleColor nextColor = schedulePort.findFirstByChildIdOrderByCreatedAtDesc(childId)
			.map(Schedule::getScheduleColor)
			.map(ScheduleColor::next)
			.orElse(ScheduleColor.SCHEDULE1);

		return new DefaultScheduleContentResponse(nextColor, nextColor.getColorCode());
	}

	@Override
	@Transactional
	public ScheduleTabResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		if (startDate.isAfter(endDate) || endDate.isBefore(startDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		List<Schedule> schedules = schedulePort.findAllByChildId(childId);
		if (schedules.isEmpty()) {
			return ScheduleTabResponse.of(false, List.of(), List.of());
		}

		List<Long> scheduleIds = schedules.stream().map(Schedule::getId).toList();

		boolean isFireLitToday = detailPort.existsStoneUsedToday(scheduleIds, LocalDate.now(clock));

		List<Long> recurringIds = schedules.stream().filter(Schedule::isRecurring).map(Schedule::getId).toList();
		List<Long> normalIds = schedules.stream().filter(s -> !s.isRecurring()).map(Schedule::getId).toList();

		List<RecurringScheduleDto> recurringDtos = List.of();
		if (!recurringIds.isEmpty()) {
			List<ScheduleRepeatDays> repeatDays = repeatDaysPort.findAllByScheduleIdsIn(recurringIds);

			Map<Long, List<ScheduleRepeatDays>> repeatDaysByScheduleId = repeatDays.stream()
				.filter(rd -> {
					Schedule schedule = rd.getSchedule();
					LocalDate createdWeekStart = schedule.getCreatedAt().toLocalDate().with(java.time.DayOfWeek.MONDAY);
					LocalDate queryWeekStart = startDate.with(java.time.DayOfWeek.MONDAY);
					return !createdWeekStart.isAfter(queryWeekStart);
				})
				.collect(Collectors.groupingBy(rd -> rd.getSchedule().getId()));

			recurringDtos = schedules.stream()
				.filter(Schedule::isRecurring)
				.map(schedule -> {
					List<ScheduleRepeatDays> days = repeatDaysByScheduleId.getOrDefault(schedule.getId(), List.of());
					if (days.isEmpty()) return null;

					String dayOfWeek = days.stream()
						.map(d -> d.getDayOfWeek().name())
						.sorted()
						.collect(Collectors.joining(", "));

					return new RecurringScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getScheduleColor().getColorCode(),
						dayOfWeek
					);
				})
				.filter(Objects::nonNull)
				.toList();
		}

		List<NormalScheduleDto> normalDtos = List.of();
		if (!normalIds.isEmpty()) {
			List<ScheduleDetail> details = detailPort.findAllByScheduleIdInAndDateBetween(normalIds, startDate, endDate);

			Map<Long, Schedule> scheduleById = schedules.stream()
				.collect(Collectors.toMap(Schedule::getId, s -> s));

			normalDtos = details.stream()
				.map(detail -> {
					Schedule schedule = scheduleById.get(detail.getSchedule().getId());
					return new NormalScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getScheduleColor().getColorCode(),
						detail.getDate()
					);
				})
				.toList();
		}

		return ScheduleTabResponse.of(isFireLitToday, recurringDtos, normalDtos);
	}

	private void checkIsExistsAndAccessibleByParentIdAndChildId(Long parentId, Long childId) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}
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

	private void stoneTypeCalculateAndSetter(List<ScheduleDetail> scheduleDetails, ScheduleDetail todoSchedule) {
		if (todoSchedule == null) return;
		switch (scheduleDetails.indexOf(todoSchedule) % 3) {
			case 0 -> todoSchedule.changeStoneType(StoneType.COURAGE);
			case 1 -> todoSchedule.changeStoneType(StoneType.GRIT);
			case 2 -> todoSchedule.changeStoneType(StoneType.WISDOM);
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
