package com.kiero.mission.application.port.in;

import com.kiero.mission.application.dto.MissionSuggestionResponse;

public interface MissionSuggestionUseCase {
	MissionSuggestionResponse suggestMissions(String noticeText);
}
