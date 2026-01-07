package com.kiero.schedule.presentation.dto;

import java.util.List;


public record ScheduleTabResponse(
	List<RecurringScheduleDto> recurringSchedules,
	List<NormalScheduleDto> normalSchedules
) {
	public static ScheduleTabResponse of(
		List<RecurringScheduleDto> recurringSchedules,
		List<NormalScheduleDto> normalSchedules) {
		return new ScheduleTabResponse(recurringSchedules, normalSchedules);
	}
}
