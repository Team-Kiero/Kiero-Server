package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record NormalScheduleDto(
	Long scheduleId,
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode,
	LocalDate date
) {
}
