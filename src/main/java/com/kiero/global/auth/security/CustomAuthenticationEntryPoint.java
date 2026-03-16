package com.kiero.global.auth.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.auth.jwt.application.exception.TokenErrorCode;
import com.kiero.global.response.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {

		String authorizationHeader = request.getHeader("Authorization");

		TokenErrorCode errorCode = (authorizationHeader != null && !authorizationHeader.startsWith("Bearer "))
			? TokenErrorCode.INVALID_AUTHORIZATION_HEADER
			: TokenErrorCode.JWT_TOKEN_EMPTY_ERROR;

		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(
			objectMapper.writeValueAsString(ErrorResponse.of(errorCode))
		);
	}
}
