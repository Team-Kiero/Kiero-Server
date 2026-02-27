package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleOccurrenceDto(
	Long scheduleId,
	LocalDate date,
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode
) {
}
