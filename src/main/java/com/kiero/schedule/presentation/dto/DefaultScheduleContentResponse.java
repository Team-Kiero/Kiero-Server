package com.kiero.schedule.presentation.dto;

import com.kiero.schedule.domain.enums.ScheduleColor;

public record DefaultScheduleContentResponse(
	ScheduleColor scheduleColor,
	String colorCode
) {
}
