package com.kiero.schedule.application.dto;

import java.time.LocalTime;

public record RecurringScheduleDto(
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode,
	String dayOfWeek
) {
}
