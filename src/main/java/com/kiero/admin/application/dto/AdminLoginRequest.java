package com.kiero.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
	@NotBlank(message = "아이디는 필수입니다.")
	String loginId,

	@NotBlank(message = "비밀번호는 필수입니다.")
	String password
) {
}
