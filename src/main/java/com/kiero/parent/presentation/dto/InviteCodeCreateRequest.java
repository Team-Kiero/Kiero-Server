package com.kiero.parent.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record InviteCodeCreateRequest(
        @NotBlank(message = "자녀 이름을 입력해주세요.")
        String childName
) {
}
