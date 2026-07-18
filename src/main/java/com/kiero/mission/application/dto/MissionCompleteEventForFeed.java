package com.kiero.mission.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record MissionCompleteEventForFeed(
	List<Parent> parents,
	Long childId,
	Long missionId,
	Integer amount,
	String name,
	LocalDateTime occurredAt
) {
}
