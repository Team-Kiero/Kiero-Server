package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleProgressForChildResponse(
	int scheduleCount,
	List<ScheduleProgressForChildDto> schedules
) {
}
