package com.kiero.global.auth.jwt.dto;

public record AccessTokenGenerateResponse(
	String accessToken
) {
	public static AccessTokenGenerateResponse of(String accessToken) {
		return new AccessTokenGenerateResponse(accessToken);
	}
}
