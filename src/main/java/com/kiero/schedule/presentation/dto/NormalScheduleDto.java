package com.kiero.schedule.presentation.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record NormalScheduleDto(
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode,
	LocalDate date
) {
}
