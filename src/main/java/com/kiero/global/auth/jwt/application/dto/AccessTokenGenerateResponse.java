package com.kiero.global.auth.jwt.application.dto;

public record AccessTokenGenerateResponse(
	String accessToken
) {
	public static AccessTokenGenerateResponse of(String accessToken) {
		return new AccessTokenGenerateResponse(accessToken);
	}
}
