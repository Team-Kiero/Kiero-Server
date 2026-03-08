package com.kiero.schedule.application.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;

public record ScheduleOccurrenceDto(
	Long scheduleId,
	LocalDate date,
	List<DayOfWeek> dayOfWeek,
	ScheduleStatus scheduleStatus,
	LocalTime startTime,
	LocalTime endTime,
	String name,
	String colorCode
) {
}
