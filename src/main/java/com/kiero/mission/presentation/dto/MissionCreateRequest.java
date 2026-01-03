package com.kiero.mission.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MissionCreateRequest(
        @NotBlank(message = "")
        Long childId,

        @NotBlank(message = "")
        String name,

        @NotBlank(message = "")
        int reward,

        @NotBlank(message = "")
        LocalDate dueAt
) {
}
