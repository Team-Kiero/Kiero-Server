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
import com.kiero.global.auth.security.ChildAuthentication;

import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.child.domain.Child;
import com.kiero.child.presentation.dto.ChildLoginResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final JwtTokenProvider jwtTokenProvider;
	private final TokenService tokenService;

	public ParentLoginResponse generateLoginResponse(Parent parent) {
		Collection<GrantedAuthority> authorities = List.of(parent.getRole().toGrantedAuthority());
		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(parent.getId(),
			parent.getRole(), authorities);
		String refreshToken = issueAndSaveRefreshToken(parent.getId(), authenticationToken);
		String accessToken = jwtTokenProvider.issueAccessToken(authenticationToken);

		return ParentLoginResponse.of(parent.getName(), parent.getEmail(), parent.getImage(), parent.getRole(),
			accessToken, refreshToken);
	}

	public ChildLoginResponse generateLoginResponse(Child child) {
		Collection<GrantedAuthority> authorities = List.of(child.getRole().toGrantedAuthority());
		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(child.getId(),
			child.getRole(), authorities);
		String refreshToken = issueAndSaveRefreshToken(child.getId(), authenticationToken);
		String accessToken = jwtTokenProvider.issueAccessToken(authenticationToken);

		return ChildLoginResponse.of(child.getLastName(), child.getFirstName(), child.getRole(), accessToken, refreshToken);
	}

	@Transactional
	public AccessTokenGenerateResponse generateAccessTokenFromRefreshToken(final String refreshToken) {
		validateRefreshToken(refreshToken);

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(refreshToken);
		Role role = jwtTokenProvider.getRoleFromJwt(refreshToken);
		verifyMemberIdWithStoredToken(refreshToken, memberId, role);
		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);
		log.info("Generated new access token for memberId: {}, role: {}, authorities: {}", memberId, role.getRoleName(),
			authorities);

		return AccessTokenGenerateResponse.of(jwtTokenProvider.issueAccessToken(authenticationToken));
	}

	@Transactional
	public AccessTokenGenerateResponse generateTemporaryAccessTokenFromRefreshToken(final String refreshToken, final String scope) {
		validateRefreshToken(refreshToken);

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(refreshToken);
		Role role = jwtTokenProvider.getRoleFromJwt(refreshToken);
		verifyMemberIdWithStoredToken(refreshToken, memberId, role);
		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);
		log.info("Generated new temporary access token for memberId: {}, role: {}, authorities: {}", memberId, role.getRoleName(),
			authorities);

		return AccessTokenGenerateResponse.of(jwtTokenProvider.issueTemporaryAccessToken(authenticationToken, List.of(scope)));
	}

	@Transactional
	public String reissueRefreshToken(String oldRefreshToken) {
		validateRefreshToken(oldRefreshToken);

		Long memberId = jwtTokenProvider.getMemberIdFromJwt(oldRefreshToken);
		Role role = jwtTokenProvider.getRoleFromJwt(oldRefreshToken);
		verifyMemberIdWithStoredToken(oldRefreshToken, memberId, role);

		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());
		UsernamePasswordAuthenticationToken authenticationToken = createAuthenticationToken(memberId, role,
			authorities);

		String newRefreshToken = jwtTokenProvider.issueRefreshToken(authenticationToken);
		tokenService.saveRefreshToken(memberId, newRefreshToken, role);

		log.info("Generated new refresh token for memberId: {}, role: {}, authorities: {}", memberId,
			role.getRoleName(), authorities);

		return newRefreshToken;
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
		Role memberRole = extractRole(authenticationToken);
		log.info("Issued new refresh token for memberId: {}", memberId);
		tokenService.saveRefreshToken(memberId, refreshToken, memberRole);
		return refreshToken;
	}

	private UsernamePasswordAuthenticationToken createAuthenticationToken(Long memberId, Role role,
		Collection<GrantedAuthority> authorities) {
		if (role == Role.ADMIN) {
			log.info("Creating AdminAuthentication for memberId: {}", memberId);
			return new AdminAuthentication(memberId, null, authorities);
		} else if (role == Role.PARENT) {
			log.info("Creating ParentAuthentication for memberId: {}", memberId);
			return new ParentAuthentication(memberId, null, authorities);
		} else {  // Role.CHILD
			log.info("Creating ChildAuthentication for memberId: {}", memberId);
			return new ChildAuthentication(memberId, null, authorities);
		}
	}

	private void verifyMemberIdWithStoredToken(String refreshToken, Long memberId, Role role) {
		String storedRefreshToken = tokenService.findRefreshToken(memberId, role);

		if (!refreshToken.equals(storedRefreshToken)) {
			log.error("MemberId mismatch: token does not match the stored refresh token");
			throw new KieroException(TokenErrorCode.REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR);
		}
	}

	private static Role extractRole(UsernamePasswordAuthenticationToken authenticationToken) {
		String authority = authenticationToken.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElseThrow(() -> new KieroException(TokenErrorCode.AUTHENTICATION_NOT_VALID));

		return Role.valueOf((authority.replace("ROLE_", "")));
	}

}
