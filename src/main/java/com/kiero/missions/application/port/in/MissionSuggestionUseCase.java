package com.kiero.missions.application.port.in;

import com.kiero.missions.application.dto.MissionSuggestionResponse;

public interface MissionSuggestionUseCase {
	MissionSuggestionResponse suggestMissions(String noticeText);
}
