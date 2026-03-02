package com.kiero.schedule.application.dto;

public record ScheduleUpdateEventTarget(
	Long childId,
	Long parentId
) {
}
