package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
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
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.ScheduleColor;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatus;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatusResolver;

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
}
