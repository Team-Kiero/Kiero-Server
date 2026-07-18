package com.kiero.mission.application.dto;

import java.time.LocalDate;

import com.kiero.mission.domain.Mission;

public record MissionUpdateResponse(
        Long id,
        String name,
        int reward,
        LocalDate dueAt
) {
    public static MissionUpdateResponse from(Mission mission) {
        return new MissionUpdateResponse(
                mission.getId(),
                mission.getName(),
                mission.getReward(),
                mission.getDueAt()
        );
    }
}
