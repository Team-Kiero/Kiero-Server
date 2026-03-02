package com.kiero.mission.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record MissionCompleteEvent(
	List<Parent> parents,
	Long childId,
	Integer amount,
	String name,
	LocalDateTime occurredAt
) {
}
