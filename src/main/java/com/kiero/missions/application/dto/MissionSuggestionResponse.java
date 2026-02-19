package com.kiero.missions.application.dto;

import java.time.LocalDate;
import java.util.List;

public record MissionSuggestionResponse(
        List<SuggestedMission> suggestedMissions
) {
    public static MissionSuggestionResponse of(List<SuggestedMission> missions) {
        return new MissionSuggestionResponse(missions);
    }

    public record SuggestedMission(
            String name,
            LocalDate dueAt,
            Integer reward
    ) {
    }
}
