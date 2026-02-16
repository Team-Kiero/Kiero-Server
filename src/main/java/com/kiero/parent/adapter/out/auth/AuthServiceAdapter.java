package com.kiero.parent.adapter.out.auth;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.jwt.infrastructure.auth.AuthService;
import com.kiero.parent.application.dto.ParentLoginResponse;
import com.kiero.parent.application.port.out.AuthGeneratePort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthGeneratePort {

	private final AuthService authService;

	@Override
	public ParentLoginResponse generateLoginResponse(Parent parent) {
		return authService.generateLoginResponse(parent);
	}
}