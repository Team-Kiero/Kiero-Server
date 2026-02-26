package com.kiero.schedule.application.dto;

import java.time.LocalDate;

import com.kiero.schedule.domain.enums.DayOfWeek;

public record DiscardedScheduleDto(
	Long scheduleId,
	LocalDate date,
	DayOfWeek dayOfWeek
) {
}
