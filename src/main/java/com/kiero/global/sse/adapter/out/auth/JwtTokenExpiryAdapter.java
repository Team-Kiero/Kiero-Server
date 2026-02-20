package com.kiero.global.sse.adapter.out.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.jwt.infrastructure.jwt.JwtTokenProvider;
import com.kiero.global.sse.application.port.out.TokenExpiryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenExpiryAdapter implements TokenExpiryPort {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	public LocalDateTime getExpirationDateTime(String token) {
		return jwtTokenProvider.getExpirationDateTime(token);
	}
}
