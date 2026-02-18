package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

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
	public List<Schedule> findAllByChildIdIn(List<Long> childIds) {
		return scheduleRepository.findAllByChildIdIn(childIds);
	}

	@Override
	public List<Schedule> findRecurringSchedulesToGenerateTodayDetail(LocalDateTime startOfToday, DayOfWeek todayDayOfWeek, LocalDate today) {
		return scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(startOfToday, todayDayOfWeek, today);
	}

	@Override
	public void deleteAll(List<Schedule> schedules) {
		scheduleRepository.deleteAll(schedules);
	}
}
