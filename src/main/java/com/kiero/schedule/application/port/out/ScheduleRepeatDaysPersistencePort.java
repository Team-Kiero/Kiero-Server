package com.kiero.schedule.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;

public interface ScheduleRepeatDaysPersistencePort {

	void saveAll(List<ScheduleRepeatDays> repeatDays);

	List<ScheduleRepeatDays> findAllByScheduleIdsIn(List<Long> scheduleIds);

	List<Schedule> findSchedulesToCreateTodayDetail(DayOfWeek dayOfWeek, LocalDate date);

	List<Schedule> findSchedulesByChildIdAndDayOfWeeks(Long childId, List<DayOfWeek> dayOfWeeks);

	List<Schedule> findSchedulesByChildIdAndDayOfWeekIn(Long childId, List<DayOfWeek> dayOfWeeks);

	void deleteByScheduleIn(List<Schedule> schedules);
}
