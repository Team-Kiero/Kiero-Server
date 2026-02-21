package com.kiero.global.auth.jwt.adapter.out;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.port.out.TokenCommandPort;
import com.kiero.global.auth.jwt.infrastructure.persistence.redis.TokenService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TokenServiceAdapter implements TokenCommandPort {

	private final TokenService tokenService;

	@Override
	public void deleteRefreshToken(Long memberId, Role role) {
		tokenService.deleteRefreshToken(memberId, role);
	}

	@Override
	public void deleteRefreshTokensBulk(List<Long> memberIds, Role role) {
		tokenService.deleteRefreshTokensBulk(memberIds, role);
	}
}