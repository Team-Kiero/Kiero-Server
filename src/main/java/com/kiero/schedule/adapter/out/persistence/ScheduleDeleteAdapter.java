package com.kiero.schedule.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.ScheduleDeletePort;
import com.kiero.schedule.domain.Schedule;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleDeleteAdapter implements ScheduleDeletePort {

	private final ScheduleRepository scheduleRepository;
	private final ScheduleDetailRepository scheduleDetailRepository;
	private final DiscardedScheduleRepository discardedScheduleRepository;
	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;

	@Override
	public void deleteAllByChildId(Long childId) {
		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		for (Schedule schedule : schedules) {
			scheduleRepeatDaysRepository.deleteAllByScheduleId(schedule.getId());
			discardedScheduleRepository.deleteByScheduleId(schedule.getId());
			scheduleDetailRepository.deleteAllByScheduleId(schedule.getId());
		}
		scheduleRepository.deleteAllById(schedules.stream().map(Schedule::getId).toList());
	}
}
