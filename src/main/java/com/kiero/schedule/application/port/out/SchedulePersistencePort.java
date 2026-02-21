package com.kiero.schedule.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

public interface SchedulePersistencePort {

	Schedule save(Schedule schedule);

	List<Schedule> saveAll(List<Schedule> schedules);

	List<Schedule> findAllByChildId(Long childId);

	Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId);

	List<Schedule> findRecurringSchedulesToGenerateTodayDetail(
		LocalDateTime startOfToday,
		DayOfWeek todayDayOfWeek,
		LocalDate today
	);

}