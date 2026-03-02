package com.kiero.schedule.application.dto;

import java.time.LocalTime;
import java.util.List;

import com.kiero.schedule.domain.enums.ScheduleStatus;

public record ScheduleProgressForParentDto(
	boolean isFireLitToday,
	List<ScheduleDto> schedules
) {
	public record ScheduleDto(
		String name,
		LocalTime startTime,
		LocalTime endTime,
		boolean isOngoing,
		String imageUrl,
		ScheduleStatus status
	) {}
}
