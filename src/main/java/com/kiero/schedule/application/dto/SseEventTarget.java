package com.kiero.schedule.application.dto;

public record SseEventTarget(
	Long childId,
	Long parentId
) {
}
