package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.kiero.schedule.domain.enums.ScheduleColor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleAddRequest(
	@Size(max = 10) @NotNull @NotEmpty String name,
	@NotNull @NotEmpty Boolean isRecurring,
	@NotNull @NotEmpty LocalTime startTime,
	@NotNull @NotEmpty LocalTime endTime,
	@NotNull @NotEmpty ScheduleColor scheduleColor,
	LocalDate firstOrderDate,
	String dayOfWeek,
	String dates
) {
}
