package com.kiero.mission.application.dto;

import java.time.LocalDate;

import com.kiero.mission.domain.Mission;

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
