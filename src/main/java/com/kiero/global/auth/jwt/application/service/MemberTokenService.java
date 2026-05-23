package com.kiero.global.auth.jwt.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.application.port.in.MemberTokenUseCase;
import com.kiero.global.auth.jwt.application.port.out.AuthTokenPort;
import com.kiero.global.auth.jwt.application.port.out.ParentChildRelationQueryPort;
import com.kiero.global.auth.jwt.application.port.out.TokenCommandPort;
import com.kiero.global.notification.application.port.in.FcmTokenRegistrationUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTokenService implements MemberTokenUseCase {

	private final TokenCommandPort tokenCommandPort;
	private final AuthTokenPort authTokenPort;
	private final ParentChildRelationQueryPort parentChildRelationQueryPort;
	private final FcmTokenRegistrationUseCase fcmTokenRegistrationUseCase;

	@Override
	@Transactional
	public void logout(Long memberId, Role role) {
		fcmTokenRegistrationUseCase.clearFcmToken(memberId, role);

		if (role == Role.PARENT) {
			tokenCommandPort.deleteRefreshToken(memberId, role);

			List<Long> childIds = parentChildRelationQueryPort.findChildIdsByParentId(memberId);

			if (!childIds.isEmpty()) {
				try {
					tokenCommandPort.deleteRefreshTokensBulk(childIds, Role.CHILD);
					log.info("Parent logout completed: parentId={}, deleted {} child tokens", memberId, childIds.size());
				} catch (Exception e) {
					log.warn("Failed to bulk delete child tokens: parentId={}, reason={}", memberId, e.getMessage());
				}
			} else {
				log.info("Parent logout completed: parentId={}, no child tokens to delete", memberId);
			}
			return;
		}

		// CHILD/ADMIN
		tokenCommandPort.deleteRefreshToken(memberId, role);
	}

	@Override
	@Transactional
	public AccessTokenGenerateResponse reissueAccessToken(String refreshToken) {
		return authTokenPort.generateAccessTokenFromRefreshToken(refreshToken);
	}

	@Override
	@Transactional
	public AccessTokenGenerateResponse issueSubscribeToken(String refreshToken) {
		return authTokenPort.generateSubscribeToken(refreshToken);
	}
}