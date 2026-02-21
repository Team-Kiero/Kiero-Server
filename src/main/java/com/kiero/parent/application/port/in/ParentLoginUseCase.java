package com.kiero.parent.application.port.in;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.parent.application.dto.ParentLoginResponse;

public interface ParentLoginUseCase {
	ParentLoginResponse loginWithAuthorizationCode(String authorizationCode, SocialLoginRequest request);
	ParentLoginResponse loginWithKakaoAccessToken(String kakaoAccessToken);
}