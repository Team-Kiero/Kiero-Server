package com.kiero.global.auth.jwt.adapter.out;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.jwt.application.port.out.AuthTokenPort;
import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.infrastructure.auth.AuthService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthTokenPort {

	private final AuthService authService;

	@Override
	public AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(String refreshToken) {
		return authService.generateAccessTokenFromRefreshToken(refreshToken);
	}

	@Override
	public String reissueRefreshToken(String refreshToken) {
		return authService.reissueRefreshToken(refreshToken);
	}

	@Override
	public AccessTokenGenerateResponse generateSubscribeToken(String refreshToken) {
		return authService.generateSubscribeToken(refreshToken);
	}
}