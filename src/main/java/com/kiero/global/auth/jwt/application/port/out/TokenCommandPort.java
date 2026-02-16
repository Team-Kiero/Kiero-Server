package com.kiero.global.auth.jwt.application.port.out;

import java.util.List;

import com.kiero.global.auth.enums.Role;

public interface TokenCommandPort {
	void deleteRefreshToken(Long memberId, Role role);
	void deleteRefreshTokensBulk(List<Long> memberIds, Role role);
}