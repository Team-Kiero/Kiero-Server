package com.kiero.mission.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MissionCreateRequest(
        @NotBlank(message = "미션 이름은 필수입니다.")
        String name,

        @NotNull(message = "보상은 필수입니다.")
        @Positive(message = "보상은 양수여야 합니다.")
        Integer reward,

        @NotNull(message = "마감일은 필수입니다.")
        LocalDate dueAt
) {
}
