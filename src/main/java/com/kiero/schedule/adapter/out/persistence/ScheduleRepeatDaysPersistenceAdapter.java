package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleRepeatDaysPersistenceAdapter implements ScheduleRepeatDaysPersistencePort {

	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;

	@Override
	public void saveAll(List<ScheduleRepeatDays> repeatDays) {
		scheduleRepeatDaysRepository.saveAll(repeatDays);
	}

	@Override
	public List<ScheduleRepeatDays> findAllByScheduleIdsIn(List<Long> scheduleIds) {
		return scheduleRepeatDaysRepository.findAllByScheduleIdsIn(scheduleIds);
	}

	@Override
	public List<DayOfWeek> findDayOfWeeksByScheduleId(Long scheduleId) {
		return scheduleRepeatDaysRepository.findDayOfWeeksByScheduleId(scheduleId);
	}

	@Override
	public List<Schedule> findSchedulesToCreateTodayDetail(DayOfWeek dayOfWeek, LocalDate date) {
		return scheduleRepeatDaysRepository.findSchedulesToCreateTodayDetail(dayOfWeek, date);
	}

	@Override
	public List<Schedule> findSchedulesByChildIdAndDayOfWeeks(Long childId, List<DayOfWeek> dayOfWeeks) {
		return scheduleRepeatDaysRepository.findSchedulesByChildIdAndDayOfWeeks(childId, dayOfWeeks);
	}

	@Override
	public List<Schedule> findSchedulesByChildIdAndDayOfWeekIn(Long childId, List<DayOfWeek> dayOfWeeks) {
		return scheduleRepeatDaysRepository.findSchedulesByChildIdAndDayOfWeekIn(childId, dayOfWeeks);
	}
}