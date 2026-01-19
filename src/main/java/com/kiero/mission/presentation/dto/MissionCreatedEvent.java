package com.kiero.mission.presentation.dto;

import java.time.LocalDateTime;

public record MissionCreatedEvent(
	Long parentId,
	Long childId,
	String missionName,
	Integer reward,
	LocalDateTime occurredAt
) {
}
