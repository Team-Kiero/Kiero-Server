package com.kiero.parent.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
	@NotBlank(message = "identityToken이 입력되지 않았습니다.")
	String identityToken,
	@NotBlank(message = "authorizationCode가 입력되지 않았습니다.")
	String authorizationCode,
	String name
) {
}
