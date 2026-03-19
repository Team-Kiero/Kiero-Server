package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleProgressForChildResponse(
	int scheduleCount,
	boolean isFireLitToday,
	List<ScheduleProgressForChildDto> schedules
) {
}
