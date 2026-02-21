package com.kiero.global.auth.jwt.application.port.out;

import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;

public interface AuthTokenPort {
	AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(String refreshToken);
	String reissueRefreshToken(String refreshToken);
	AccessTokenGenerateResponse generateSubscribeToken(String refreshToken);
}