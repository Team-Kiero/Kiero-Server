package com.kiero.mission.presentation.dto;

public record MissionItemResponse(
    Long id,
    String name,
    int reward,
    boolean isCompleted
) {
    public static MissionItemResponse from(MissionResponse response) {
        return new MissionItemResponse(
            response.id(),
            response.name(),
            response.reward(),
            response.isCompleted()
        );
    }
}
