package com.kiero.schedule.application.dto;

import java.time.LocalTime;

import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;

public record ScheduleProgressForChildDto(
	String name,
	LocalTime startTime,
	LocalTime endTime,
	boolean isOngoing,
	StoneType stoneType,
	ScheduleStatus status
) {
}

