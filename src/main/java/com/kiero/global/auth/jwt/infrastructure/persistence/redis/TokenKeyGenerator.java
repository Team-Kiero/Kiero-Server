package com.kiero.global.auth.jwt.infrastructure.persistence.redis;

import com.kiero.global.auth.enums.Role;

public final class TokenKeyGenerator {
	private TokenKeyGenerator() {}

	public static String refreshKey(Long memberId, Role role) {
		return "refresh:" + role.name() + ":" + memberId;
	}
}