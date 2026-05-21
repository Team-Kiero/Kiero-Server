package com.kiero.parent.adapter.out.social;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.apple.AppleAuthApiClient;
import com.kiero.global.auth.client.apple.AppleClientSecretGenerator;
import com.kiero.global.auth.client.apple.dto.AppleTokenResponse;
import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.AppleAuthPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleAuthAdapter implements AppleAuthPort {

	@Value("${apple.bundle-id}")
	private String bundleId;

	private final AppleAuthApiClient appleAuthApiClient;
	private final AppleClientSecretGenerator appleClientSecretGenerator;

	@Override
	public String exchangeAuthorizationCode(String authorizationCode) {
		try {
			String clientSecret = appleClientSecretGenerator.generate();
			AppleTokenResponse response = appleAuthApiClient.exchangeAuthorizationCode(
				bundleId,
				clientSecret,
				authorizationCode,
				"authorization_code"
			);

			if (response.refreshToken() == null || response.refreshToken().isBlank()) {
				log.error("Apple authorization code 교환 결과 refresh token이 없습니다.");
				throw new KieroException(OAuthErrorCode.APPLE_AUTH_CODE_EXCHANGE_FAILED);
			}

			return response.refreshToken();
		} catch (KieroException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apple authorization code 교환 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.APPLE_AUTH_CODE_EXCHANGE_FAILED);
		}
	}
}
