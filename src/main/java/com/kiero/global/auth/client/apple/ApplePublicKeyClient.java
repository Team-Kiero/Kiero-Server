package com.kiero.global.auth.client.apple;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.kiero.global.auth.client.apple.dto.ApplePublicKeyResponse;

@FeignClient(name = "applePublicKeyClient", url = "https://appleid.apple.com")
public interface ApplePublicKeyClient {

	@GetMapping("/auth/keys")
	ApplePublicKeyResponse getApplePublicKeys();
}
