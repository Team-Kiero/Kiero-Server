package com.kiero.schedule.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.schedule.domain.Schedule;

public interface SchedulePersistencePort {

	Schedule save(Schedule schedule);

	List<Schedule> saveAll(List<Schedule> schedules);

	List<Schedule> findAllByChildId(Long childId);

	Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId);

	Optional<Schedule> findById(Long scheduleId);

}