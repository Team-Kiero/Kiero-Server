package com.kiero.mission.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MissionBulkCreateRequest(
        @NotEmpty(message = "미션 목록은 비어있을 수 없습니다.")
        @Valid
        List<MissionItem> missions
) {
    public record MissionItem(
            @NotNull(message = "미션 이름은 필수입니다.")
            String name,

            @NotNull(message = "보상은 필수입니다.")
            @Positive(message = "보상은 양수여야 합니다.")
            Integer reward,

            @NotNull(message = "마감일은 필수입니다.")
            java.time.LocalDate dueAt
    ) {
    }
}
