package com.kiero.global.auth.redis;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.redis.util.TokenKeyGenerator;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;

@RedisHash(value = "refreshToken", timeToLive = 60L * 60 * 24 * 14)
@Getter
@AllArgsConstructor(staticName = "of")
public class Token {

	@Id
	private String id;

	private Long memberId;

	private String refreshToken;

	private String role;

	public static Token of(Long memberId, String refreshToken, Role role) {
		String key = TokenKeyGenerator.refreshKey(memberId, role);
		return new Token(key, memberId,refreshToken, role.name());
	}
}
