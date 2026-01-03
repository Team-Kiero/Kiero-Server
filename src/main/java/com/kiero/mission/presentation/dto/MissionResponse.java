package com.kiero.mission.presentation.dto;

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
}
