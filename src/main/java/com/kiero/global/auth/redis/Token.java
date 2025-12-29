package com.kiero.global.auth.redis;

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import jakarta.persistence.Id;
import lombok.Getter;

@RedisHash(value = "refreshToken", timeToLive = 60L * 60 * 24 * 14)
@Getter
public class Token {

	@Id
	private Long id;

	@Indexed
	private String refreshToken;

	protected Token() {
	}

	public Token(Long id, String refreshToken) {
		this.id = id;
		this.refreshToken = refreshToken;
	}

	public static Token of(Long id, String refreshToken) {
		return new Token(id, refreshToken);
	}
}
