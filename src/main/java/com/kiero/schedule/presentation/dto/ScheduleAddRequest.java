package com.kiero.schedule.presentation.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleAddRequest(
	@Size(max = 10) @NotNull String name,
	@NotNull Boolean isRecurring,
	@NotNull LocalTime startTime,
	@NotNull LocalTime endTime,
	@NotNull String colorCode,
	String dayOfWeek,
	LocalDate date
) {
}
