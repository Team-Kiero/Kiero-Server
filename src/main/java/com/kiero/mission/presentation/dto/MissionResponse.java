package com.kiero.mission.presentation.dto;

import com.kiero.mission.domain.Mission;

import java.time.LocalDate;

public record MissionResponse(
        Long id,
        String name,
        int reward,
        LocalDate dueAt,
        boolean isCompleted
) {
    public static MissionResponse of(Long id, String name, int reward, LocalDate dueAt, boolean isCompleted) {
        return new MissionResponse(id, name, reward, dueAt, isCompleted);
    }

    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getName(),
                mission.getReward(),
                mission.getDueAt(),
                mission.isCompleted()
        );
    }
}
