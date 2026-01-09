package com.kiero.mission.presentation.dto;

import java.time.LocalDateTime;

public record MissionCompleteEvent(
	Long childId,
	Integer amount,
	String name,
	LocalDateTime occurredAt
) {
}
