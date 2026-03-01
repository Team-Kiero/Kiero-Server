package com.kiero.schedule.application.dto;

public record ScheduleStatusAutomaticallyUpdatedEvent(
	SseEventTarget target
) {
}
