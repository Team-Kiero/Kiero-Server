package com.kiero.mission.application.dto;

public record MissionCreatedEvent(
	Long childId,
	String missionName,
	Integer reward
) {
}
