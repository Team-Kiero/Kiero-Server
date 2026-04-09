package com.kiero.admin.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.dto.AdminLoginRequest;
import com.kiero.admin.application.dto.AdminLoginResponse;
import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminLoginUseCase;
import com.kiero.admin.application.port.out.AdminAuthGeneratePort;
import com.kiero.admin.application.port.out.AdminLoadPort;
import com.kiero.admin.domain.Admin;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLoginService implements AdminLoginUseCase {

	private final AdminLoadPort adminLoadPort;
	private final AdminAuthGeneratePort adminAuthGeneratePort;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional(readOnly = true)
	public AdminLoginResponse login(AdminLoginRequest request) {
		Admin admin = adminLoadPort.findByLoginId(request.loginId())
			.orElseThrow(() -> new KieroException(AdminErrorCode.INVALID_ADMIN_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
			throw new KieroException(AdminErrorCode.INVALID_ADMIN_CREDENTIALS);
		}

		return adminAuthGeneratePort.generateLoginResponse(admin);
	}
}
