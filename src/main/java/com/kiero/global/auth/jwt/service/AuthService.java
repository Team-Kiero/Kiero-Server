package com.kiero.global.auth.jwt.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.enums.JwtValidationType;
import com.kiero.global.auth.jwt.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.exception.TokenErrorCode;
import com.kiero.global.auth.security.AdminAuthentication;
import com.kiero.global.auth.security.ParentAuthentication;

import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.presentation.dto.ParentLoginResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;

	public ParentLoginResponse generateLoginResponse(Parent member) {
		Collection<GrantedAuthority> authorities = List.of(member.getRole().toGrantedAuthority());
		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(member.getId(),
			member.getRole(), authorities);
		String refreshToken = issueAndSaveRefreshToken(member.getId(), authenticationToken);
		String accessToken = jwtTokenProvider.issueAccessToken(authenticationToken);

		return ParentLoginResponse.of(member.getName(), member.getEmail(), member.getImage(), member.getRole(), accessToken, refreshToken);
	}

	@Transactional
	public AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(final String refreshToken) {
		validateRefreshToken(refreshToken);

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(refreshToken);
		verifyMemberIdWithStoredToken(refreshToken, memberId);

		Role role = jwtTokenProvider.getRoleFromJwt(refreshToken);
		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);
		log.info("Generated new access token for memberId: {}, role: {}, authorities: {}", memberId, role.getRoleName(),
			authorities);

		return AccessTokenGenerateResponse.of(jwtTokenProvider.issueAccessToken(authenticationToken));
	}

	@Transactional
	public String generateRefreshTokenFromOldRefreshToken(String oldRefreshToken, Role role) {
		validateRefreshToken(oldRefreshToken);

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(oldRefreshToken);
		verifyMemberIdWithStoredToken(oldRefreshToken, memberId);

		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);
		log.info("Generated new refresh token for memberId: {}, role: {}, authorities: {}", memberId,
			role.getRoleName(), authorities);


		return issueAndSaveRefreshToken(memberId, authenticationToken);
	}

	private void validateRefreshToken(String refreshToken) {
		JwtValidationType validationType = jwtTokenProvider.validateToken(refreshToken);

		if (!validationType.equals(JwtValidationType.VALID_JWT)) {
			throw switch (validationType) {
				case EXPIRED_JWT_TOKEN -> new KieroException(TokenErrorCode.REFRESH_TOKEN_EXPIRED_ERROR);
				case INVALID_JWT_TOKEN -> new KieroException(TokenErrorCode.INVALID_REFRESH_TOKEN_ERROR);
				case INVALID_JWT_SIGNATURE -> new KieroException(TokenErrorCode.REFRESH_TOKEN_SIGNATURE_ERROR);
				case UNSUPPORTED_JWT_TOKEN -> new KieroException(TokenErrorCode.UNSUPPORTED_REFRESH_TOKEN_ERROR);
				case EMPTY_JWT -> new KieroException(TokenErrorCode.REFRESH_TOKEN_EMPTY_ERROR);
				default -> new KieroException(TokenErrorCode.UNKNOWN_REFRESH_TOKEN_ERROR);
			};
		}
	}

	private String issueAndSaveRefreshToken(Long memberId, UsernamePasswordAuthenticationToken authenticationToken) {
		String refreshToken = jwtTokenProvider.issueRefreshToken(authenticationToken);
		log.info("Issued new refresh token for memberId: {}", memberId);
		tokenService.saveRefreshToken(memberId, refreshToken);
		return refreshToken;
	}

	private UsernamePasswordAuthenticationToken createAuthenticationToken(Long memberId, Role role,
		Collection<GrantedAuthority> authorities) {
		if (role == Role.ADMIN) {
			log.info("Creating AdminAuthentication for memberId: {}", memberId);
			return new AdminAuthentication(memberId, null, authorities);
		} else {
			log.info("Creating MemberAuthentication for memberId: {}", memberId);
			return new ParentAuthentication(memberId, null, authorities);
		}
	}

	private void verifyMemberIdWithStoredToken(String refreshToken, Long memberId) {
		Long storedMemberId = tokenService.findIdByRefreshToken(refreshToken);

		if (!memberId.equals(storedMemberId)) {
			log.error("MemberId mismatch: token does not match the stored refresh token");
			throw new KieroException(TokenErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR);
		}
	}

}
