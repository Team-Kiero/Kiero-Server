package com.kiero.mission.presentation.dto;

import com.kiero.mission.domain.Mission;

public record MissionItemResponse(
    Long id,
    String name,
    int reward,
    boolean isCompleted
) {
    public static MissionItemResponse from(Mission mission) {
        return new MissionItemResponse(
            mission.getId(),
            mission.getName(),
            mission.getReward(),
            mission.isCompleted()
        );
    }

    public static MissionItemResponse from(MissionResponse response) {
        return new MissionItemResponse(
            response.id(),
            response.name(),
            response.reward(),
            response.isCompleted()
        );
    }
}
