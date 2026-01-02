package com.kiero.child.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ChildSignupRequest(
        @NotBlank(message = "초대 코드를 입력해주세요.")
        String inviteCode,

        @NotBlank(message = "이름을 입력해주세요.")
        String name
) {
}
