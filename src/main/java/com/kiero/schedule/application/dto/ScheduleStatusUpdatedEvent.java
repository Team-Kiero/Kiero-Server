package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleStatusUpdatedEvent(
	Long childId,
	List<Long> parentIds
) {
}
