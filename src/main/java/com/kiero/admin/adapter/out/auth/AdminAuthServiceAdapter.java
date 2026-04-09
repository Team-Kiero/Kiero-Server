package com.kiero.admin.adapter.out.auth;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.dto.AdminLoginResponse;
import com.kiero.admin.application.port.out.AdminAuthGeneratePort;
import com.kiero.admin.domain.Admin;
import com.kiero.global.auth.jwt.application.service.AuthService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminAuthServiceAdapter implements AdminAuthGeneratePort {

	private final AuthService authService;

	@Override
	public AdminLoginResponse generateLoginResponse(Admin admin) {
		return authService.generateLoginResponse(admin);
	}
}
