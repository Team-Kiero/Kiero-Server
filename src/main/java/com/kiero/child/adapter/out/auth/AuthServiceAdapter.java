package com.kiero.child.adapter.out.auth;

import org.springframework.stereotype.Component;

import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.application.port.out.AuthGeneratePort;
import com.kiero.child.domain.Child;
import com.kiero.global.auth.jwt.infrastructure.auth.AuthService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthServiceAdapter implements AuthGeneratePort {

	private final AuthService authService;

	@Override
	public ChildLoginResponse generateLoginResponse(Child child) {
		return authService.generateLoginResponse(child);
	}
}