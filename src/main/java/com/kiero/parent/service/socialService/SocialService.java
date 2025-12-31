package com.kiero.parent.service.socialService;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;

public interface SocialService {
	SocialLoginResponse login(
		final String authorizationToken,
		final SocialLoginRequest storeSocialLoginRequest);
}
