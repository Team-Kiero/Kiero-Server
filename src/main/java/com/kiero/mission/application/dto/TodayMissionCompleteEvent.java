package com.kiero.mission.application.dto;

import java.util.List;

public record TodayMissionCompleteEvent(
	List<Long> parentIds,
	Long childId
) {
}
