package com.kiero.schedule.application.dto;

import java.time.LocalTime;
import java.util.List;

import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;

public record ChildScheduleProgressResponse(
	int scheduleCount,
	List<ScheduleDto> schedules
) {
	public record ScheduleDto(
		String name,
		LocalTime startTime,
		LocalTime endTime,
		boolean isOngoing,
		StoneType stoneType,
		ScheduleStatus status
	) {
	}
}
