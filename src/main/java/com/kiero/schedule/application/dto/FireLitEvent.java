package com.kiero.schedule.application.dto;

import java.time.LocalDateTime;

public record FireLitEvent(
	Long childId,
	Integer amount,
	LocalDateTime occurredAt
) {
}
