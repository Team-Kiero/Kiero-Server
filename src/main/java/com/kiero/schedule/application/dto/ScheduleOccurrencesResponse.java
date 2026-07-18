package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleOccurrencesResponse(
	boolean isFireLit,
	List<ScheduleOccurrenceDto> items
) {
	public static ScheduleOccurrencesResponse of(boolean isFireLit, List<ScheduleOccurrenceDto> items) {
		return new ScheduleOccurrencesResponse(isFireLit, items);
	}
}