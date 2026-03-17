package com.kiero.schedule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchedulePersistenceAdapter implements SchedulePersistencePort {

	private final ScheduleRepository scheduleRepository;

	@Override
	public Schedule save(Schedule schedule) {
		return scheduleRepository.save(schedule);
	}

	@Override
	public List<Schedule> saveAll(List<Schedule> schedules) {
		return scheduleRepository.saveAll(schedules);
	}

	@Override
	public List<Schedule> findAllByChildId(Long childId) {
		return scheduleRepository.findAllByChildId(childId);
	}

	@Override
	public Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId) {
		return scheduleRepository.findFirstByChildIdOrderByCreatedAtDesc(childId);
	}

	@Override
	public Optional<Schedule> findById(Long scheduleId) {
		return scheduleRepository.findById(scheduleId);
	}

	@Override
	public void deleteById(Long scheduleId) {
		scheduleRepository.deleteById(scheduleId);
	}

}
