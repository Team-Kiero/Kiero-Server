package com.kiero.schedule.application.dto;

import com.kiero.schedule.domain.enums.ScheduleColor;

public record DefaultScheduleContentResponse(
	ScheduleColor scheduleColor,
	String colorCode
) {
}
