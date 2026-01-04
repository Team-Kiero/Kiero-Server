package com.kiero.schedule.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.enums.DayOfWeek;
import com.kiero.schedule.exception.ScheduleErrorCode;
import com.kiero.schedule.presentation.dto.ScheduleAddRequest;
import com.kiero.schedule.repository.ScheduleRepeatDaysRepository;
import com.kiero.schedule.repository.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ParentRepository parentRepository;
	private final ChildRepository childRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;

	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(()-> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(()-> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		Schedule schedule = Schedule.create(parent, child, request.name(), request.startTime(), request.endTime(), request.colorCode(), request.isRecurring());
		Schedule savedSchedule = scheduleRepository.save(schedule);

		if (request.isRecurring()
			&& request.dayOfWeek()!=null
			&& !request.dayOfWeek().isBlank()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, savedSchedule))
				.toList();

			scheduleRepeatDaysRepository.saveAll(repeatDays);
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
}
