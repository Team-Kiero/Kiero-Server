package com.kiero.mission.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MissionSuggestionRequest(
        @NotBlank(message = "알림장 내용은 필수입니다.")
        @Size(min = 10, max = 1000, message = "알림장 내용은 10자 이상 1000자 이하로 입력해주세요.")
        String noticeText
) {
}
