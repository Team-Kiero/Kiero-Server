package com.kiero.mission.presentation.dto;

import java.util.List;

public record MissionSuggestionResponse(
        List<String> suggestedMissions
) {
    public static MissionSuggestionResponse of(List<String> missions) {
        return new MissionSuggestionResponse(missions);
    }
}
