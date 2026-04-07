package com.kiero.admin.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.kiero.mission.domain.Mission;

public record AdminMissionResponse(
	Long id,
	String name,
	int reward,
	LocalDate dueAt,
	boolean isCompleted,
	Long parentId,
	Long childId,
	LocalDateTime createdAt
) {

	public static AdminMissionResponse from(Mission mission) {
		return new AdminMissionResponse(
			mission.getId(),
			mission.getName(),
			mission.getReward(),
			mission.getDueAt(),
			mission.isCompleted(),
			mission.getParent().getId(),
			mission.getChild().getId(),
			mission.getCreatedAt()
		);
	}
}
