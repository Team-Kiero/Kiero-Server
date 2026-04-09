package com.kiero.admin.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.admin.application.dto.AdminLoginRequest;
import com.kiero.admin.application.dto.AdminLoginResponse;
import com.kiero.admin.application.exception.AdminSuccessCode;
import com.kiero.admin.application.port.in.AdminLoginUseCase;
import com.kiero.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

	private final AdminLoginUseCase adminLoginUseCase;

	@PostMapping("/login")
	public ResponseEntity<SuccessResponse<AdminLoginResponse>> login(
		@Valid @RequestBody AdminLoginRequest request
	) {
		AdminLoginResponse response = adminLoginUseCase.login(request);

		return ResponseEntity.ok()
			.body(SuccessResponse.of(AdminSuccessCode.LOGIN_SUCCESS, response));
	}
}
