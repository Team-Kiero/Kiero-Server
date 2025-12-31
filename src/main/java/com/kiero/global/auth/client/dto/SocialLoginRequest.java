package com.kiero.global.auth.client.dto;

import com.kiero.global.auth.client.enums.Platform;
import com.kiero.global.auth.client.enums.Provider;

import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
	@NotNull(message = "소셜 로그인 종류가 입력되지 않았습니다.")
	Provider provider,
	@NotNull(message = "플랫폼 종류가 입력되지 않았습니다.")
	Platform platform
) {
}