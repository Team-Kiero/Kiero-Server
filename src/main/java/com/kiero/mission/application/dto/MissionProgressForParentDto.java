package com.kiero.mission.application.dto;

import java.util.List;

public record MissionProgressForParentDto(
	List<MissionDto> completeMissions,
	List<MissionDto> incompleteMissions
) {
	public record MissionDto(
		String name,
		int reward
	) {}
}
