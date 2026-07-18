package com.kiero.parent.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteCodeCreateRequest(
        @NotBlank(message = "자녀 성을 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "자녀 성에 특수문자, 공백, 이모지를 사용할 수 없습니다.")
        String childLastName,

        @NotBlank(message = "자녀 이름을 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "자녀 이름에 특수문자, 공백, 이모지를 사용할 수 없습니다.")
        String childFirstName
) {
}
