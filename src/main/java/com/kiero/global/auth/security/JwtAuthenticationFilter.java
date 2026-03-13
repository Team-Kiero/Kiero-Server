package com.kiero.global.auth.security;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.exception.TokenErrorCode;
import com.kiero.global.auth.jwt.infrastructure.JwtTokenProvider;
import com.kiero.global.auth.jwt.infrastructure.JwtValidationType;
import com.kiero.global.exception.KieroException;
import com.kiero.global.response.code.ErrorCode;
import com.kiero.global.response.dto.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		String token = getJwtFromRequest(request);

		if (!StringUtils.hasText(token)) {
			// 토큰 없는 요청은 그냥 다음 필터로
			filterChain.doFilter(request, response);
			return;
		}

		JwtValidationType validationType = jwtTokenProvider.validateToken(token);
		if (validationType.isValid()) {
			setAuthentication(token, request);
			filterChain.doFilter(request, response);
		} else {
			handleInvalidToken(validationType, response);
		}
	}

	private void setAuthentication(String token, HttpServletRequest request) {
		Long memberId = jwtTokenProvider.getMemberIdFromJwt(token);
		Role role = jwtTokenProvider.getRoleFromJwt(token);

		Collection<GrantedAuthority> authorities = List.of(role.toGrantedAuthority());

		UsernamePasswordAuthenticationToken authentication;
		if (role == Role.ADMIN) {
			authentication = new AdminAuthentication(memberId, null, authorities);
		} else if (role == Role.PARENT) {
			authentication = new ParentAuthentication(memberId, null, authorities);
		} else if (role == Role.CHILD) {
			authentication = new ChildAuthentication(memberId, null, authorities);
		} else {
            throw new KieroException(ErrorCode.ACCESS_DENIED);
        }

		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void handleInvalidToken(JwtValidationType validationType, HttpServletResponse response) throws IOException {

		TokenErrorCode errorCode = switch (validationType) {
			case EXPIRED_JWT_TOKEN -> TokenErrorCode.JWT_TOKEN_EXPIRED_ERROR;
			case INVALID_JWT_TOKEN -> TokenErrorCode.INVALID_JWT_TOKEN_ERROR;
			case INVALID_JWT_SIGNATURE -> TokenErrorCode.JWT_TOKEN_SIGNATURE_ERROR;
			case UNSUPPORTED_JWT_TOKEN -> TokenErrorCode.UNSUPPORTED_JWT_TOKEN_ERROR;
			case EMPTY_JWT -> TokenErrorCode.JWT_TOKEN_EMPTY_ERROR;
			default -> TokenErrorCode.UNKNOWN_JWT_TOKEN_ERROR;
		};

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType("application/json;charset=UTF-8");

		ObjectMapper objectMapper = new ObjectMapper();
		response.getWriter().write(
			objectMapper.writeValueAsString(
				ErrorResponse.of(errorCode)
			)
		);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring("Bearer ".length());
		}
		return null;
	}
}
