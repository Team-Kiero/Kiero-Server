package com.kiero.schedule.application.dto;

import java.util.List;


public record ScheduleTabResponse(
	boolean isFireLit,
	List<RecurringScheduleDto> recurringSchedules,
	List<NormalScheduleDto> normalSchedules
) {
	public static ScheduleTabResponse of(
		boolean isFireLit,
		List<RecurringScheduleDto> recurringSchedules,
		List<NormalScheduleDto> normalSchedules) {
		return new ScheduleTabResponse(isFireLit, recurringSchedules, normalSchedules);
	}
}
