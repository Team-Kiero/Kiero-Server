package com.kiero.admin.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminScheduleLoadPort;
import com.kiero.schedule.adapter.out.persistence.ScheduleRepository;
import com.kiero.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminSchedulePersistenceAdapter implements AdminScheduleLoadPort {

	private final ScheduleRepository scheduleRepository;

	@Override
	public List<Schedule> findAllByChildId(Long childId) {
		return scheduleRepository.findAllByChildId(childId);
	}

	@Override
	public Optional<Schedule> findById(Long scheduleId) {
		return scheduleRepository.findById(scheduleId);
	}
}
