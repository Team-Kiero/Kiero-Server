package com.kiero.schedule.application.dto;

public record ScheduleEventTarget(
	Long childId,
	Long parentId
) {
}
