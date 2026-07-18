package com.kiero.global.auth.jwt.application.port.out;

import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;

public interface AuthTokenPort {
	AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(String refreshToken);
	AccessTokenGenerateResponse generateSubscribeToken(String refreshToken);
}