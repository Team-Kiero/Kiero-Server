package com.kiero.schedule.presentation.dto;

import java.time.LocalDateTime;

public record FireLitEvent(
	Long childId,
	Integer amount,
	LocalDateTime occurredAt
) {
}
