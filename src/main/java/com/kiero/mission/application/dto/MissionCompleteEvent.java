package com.kiero.mission.application.dto;

import java.util.List;

public record MissionCompleteEvent(
	List<Long> parentIds,
	Long childId,
	Long missionId,
	String missionName
) {
}
