package com.kiero.parent.adapter.out.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kiero.parent.application.port.out.ParentWithdrawNotificationPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ParentWithdrawRedisAdapter implements ParentWithdrawNotificationPort {

	private static final String KEY_PREFIX = "child:";

	@Value("${jwt.access-token-expire-time}")
	private long accessTokenExpireTimeMs;

	private final ParentWithdrawalRepository parentWithdrawalRepository;

	@Override
	public void storeWithdrawalMarker(Long childId) {
		long ttlSeconds = accessTokenExpireTimeMs / 1000;
		parentWithdrawalRepository.save(ParentWithdrawal.of(KEY_PREFIX + childId, childId, ttlSeconds));
	}
}
