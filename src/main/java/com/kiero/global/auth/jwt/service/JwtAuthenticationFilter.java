package com.kiero.global.auth.jwt.service;

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

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.enums.JwtValidationType;
import com.kiero.global.auth.security.ParentAuthentication;

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
		UsernamePasswordAuthenticationToken authentication =
			new ParentAuthentication(memberId.toString(), null, authorities);

		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void handleInvalidToken(JwtValidationType validationType, HttpServletResponse response) {
		if (validationType == JwtValidationType.EXPIRED_JWT_TOKEN) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring("Bearer ".length());
		}
		return null;
	}
}
