package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleSkippedEvent(
	List<Long> parentIds,
	Long childId
) {
}
