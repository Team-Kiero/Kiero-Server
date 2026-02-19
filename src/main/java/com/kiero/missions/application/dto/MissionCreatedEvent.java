package com.kiero.missions.application.dto;

public record MissionCreatedEvent(
	Long childId,
	String missionName,
	Integer reward
) {
}
