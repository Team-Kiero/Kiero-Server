package com.kiero.schedule.presentation.dto;

import java.time.LocalTime;

public record RecurringScheduleDto(
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode,
	String dayOfWeek
) {
}
