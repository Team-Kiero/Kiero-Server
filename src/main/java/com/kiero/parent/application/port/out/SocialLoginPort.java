package com.kiero.parent.application.port.out;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;

public interface SocialLoginPort {
	SocialLoginResponse login(String authorizationCode, SocialLoginRequest request);
	SocialLoginResponse loginWithAccessToken(String accessToken);
}