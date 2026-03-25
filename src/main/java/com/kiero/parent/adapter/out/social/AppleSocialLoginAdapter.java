package com.kiero.parent.adapter.out.social;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.apple.AppleJwtValidator;
import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.parent.application.port.out.AppleSocialLoginPort;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleSocialLoginAdapter implements AppleSocialLoginPort {

	private final AppleJwtValidator appleJwtValidator;

	@Override
	public SocialLoginResponse loginWithIdentityToken(String identityToken) {
		Claims claims = appleJwtValidator.validateAndExtractClaims(identityToken);

		String sub = claims.getSubject();
		String email = claims.get("email", String.class);

		log.info("Apple 로그인 성공 - sub: {}", sub);

		return SocialLoginResponse.of(sub, Provider.APPLE, null, email, null);
	}
}
