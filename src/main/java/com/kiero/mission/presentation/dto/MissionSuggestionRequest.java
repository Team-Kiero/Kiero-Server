package com.kiero.mission.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record MissionSuggestionRequest(
        @NotBlank(message = "알림장 내용은 필수입니다.")
        String noticeText
) {
}
