package com.kiero.global.auth.client.apple;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kiero.global.auth.client.apple.dto.AppleTokenResponse;

@FeignClient(name = "appleAuthApiClient", url = "https://appleid.apple.com")
public interface AppleAuthApiClient {

	@PostMapping(value = "/auth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	AppleTokenResponse exchangeAuthorizationCode(
		@RequestParam("client_id") String clientId,
		@RequestParam("client_secret") String clientSecret,
		@RequestParam("code") String code,
		@RequestParam("grant_type") String grantType
	);
}
