package com.kiero.schedule.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.exception.ScheduleErrorCode;
import com.kiero.schedule.presentation.dto.NormalScheduleDto;
import com.kiero.schedule.presentation.dto.RecurringScheduleDto;
import com.kiero.schedule.presentation.dto.ScheduleAddRequest;
import com.kiero.schedule.presentation.dto.ScheduleTabResponse;
import com.kiero.schedule.repository.ScheduleDetailRepository;
import com.kiero.schedule.repository.ScheduleRepeatDaysRepository;
import com.kiero.schedule.repository.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ParentRepository parentRepository;
	private final ChildRepository childRepository;
	private final ParentChildRepository parentChildRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;
	private final ScheduleDetailRepository scheduleDetailRepository;

	@Transactional
	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		Schedule schedule = Schedule.create(parent, child, request.name(), request.startTime(), request.endTime(),
			request.colorCode(), request.isRecurring());
		Schedule savedSchedule = scheduleRepository.save(schedule);

		if (request.isRecurring()
			&& request.dayOfWeek() != null
			&& !request.dayOfWeek().isBlank()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, savedSchedule))
				.toList();

			scheduleRepeatDaysRepository.saveAll(repeatDays);
		}

		if (request.date() != null) {
			ScheduleDetail scheduleDetail = ScheduleDetail.create(request.date(), null, null, ScheduleStatus.PENDING, null,  savedSchedule);
			scheduleDetailRepository.save(scheduleDetail);
		}
	}

	@Transactional
	public ScheduleTabResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId) {

		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		if (schedules.isEmpty())
			return ScheduleTabResponse.of(List.of(), List.of());

		List<Long> recurringIds = schedules.stream()
			.filter(Schedule::isRecurring)
			.map(Schedule::getId)
			.toList();

		List<Long> normalIds = schedules.stream()
			.filter(s -> !s.isRecurring())
			.map(Schedule::getId)
			.toList();

		List<RecurringScheduleDto> recurringScheduleDtos = List.of();
		if (!recurringIds.isEmpty()) {
			List<ScheduleRepeatDays> repeatDays = scheduleRepeatDaysRepository.findAllByScheduleIdsIn(recurringIds);
			Map<Long, List<ScheduleRepeatDays>> repeatDaysByScheduleId = repeatDays.stream()
				.collect(Collectors.groupingBy(rd -> rd.getSchedule().getId()));

			recurringScheduleDtos = schedules.stream()
				.filter(Schedule::isRecurring)
				.map(schedule -> {
					List<ScheduleRepeatDays> days = repeatDaysByScheduleId.getOrDefault(schedule.getId(), List.of());

					String dayOfWeek = days.stream()
						.map(d -> d.getDayOfWeek().name())
						.sorted()
						.collect(Collectors.joining(", "));

					return new RecurringScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getColorCode(),
						dayOfWeek
					);
				})
				.toList();
		}

		List<NormalScheduleDto> normalScheduleDtos = List.of();
		if (!normalIds.isEmpty()) {
			List<ScheduleDetail> details = scheduleDetailRepository
				.findAllByScheduleIdInAndDateBetween(normalIds, startDate, endDate);

			Map<Long, Schedule> scheduleById = schedules.stream()
				.collect(Collectors.toMap(Schedule::getId, s -> s));

			normalScheduleDtos = details.stream()
				.map(detail -> {
					Schedule schedule = scheduleById.get(detail.getSchedule().getId());
					return new NormalScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getColorCode(),
						detail.getDate()
					);
				})
				.toList();
		}

		return ScheduleTabResponse.of(recurringScheduleDtos, normalScheduleDtos);

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
}
