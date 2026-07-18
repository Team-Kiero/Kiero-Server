package com.kiero.global.auth.client.apple;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.apple.dto.ApplePublicKeyResponse;
import com.kiero.global.config.CacheNames;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplePublicKeyProvider {

	private final ApplePublicKeyClient applePublicKeyClient;

	@Cacheable(value = CacheNames.APPLE_PUBLIC_KEYS, key = "'keys'")
	public ApplePublicKeyResponse getPublicKeys() {
		log.info("Apple public key를 서버에서 가져옵니다.");
		return applePublicKeyClient.getApplePublicKeys();
	}

	@CacheEvict(value = CacheNames.APPLE_PUBLIC_KEYS, allEntries = true)
	public void evictCache() {
		log.info("Apple public key 캐시를 초기화합니다.");
	}
}