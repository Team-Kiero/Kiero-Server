package com.kiero.schedule.application.dto;

import java.time.LocalTime;

import com.kiero.schedule.domain.enums.ScheduleColor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleModifyRequest(
	@Size(max = 10) @NotNull @NotEmpty String name,
	@NotNull Boolean isRecurring,
	@NotNull LocalTime startTime,
	@NotNull LocalTime endTime,
	@NotNull ScheduleColor scheduleColor,
	String dayOfWeek,
	String dates,
	Boolean isIncludeFollowing
) {
}
