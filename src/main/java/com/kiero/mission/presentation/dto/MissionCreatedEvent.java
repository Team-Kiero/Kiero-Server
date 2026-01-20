package com.kiero.mission.presentation.dto;

public record MissionCreatedEvent(
	Long childId,
	String missionName,
	Integer reward
) {
}
