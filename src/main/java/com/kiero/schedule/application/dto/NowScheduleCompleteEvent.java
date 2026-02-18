package com.kiero.schedule.application.dto;

import java.time.LocalDateTime;

public record NowScheduleCompleteEvent(
	Long childId,
	String name,
	String imageUrl,
	LocalDateTime occurredAt
) {
}
