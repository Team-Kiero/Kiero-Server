package com.kiero.parent.application.dto;

import com.kiero.global.auth.enums.Role;

public record ParentLoginResponse(
	Long id,
	String name,
	String email,
	String image,
	Role role,
	String accessToken,
	String refreshToken
) {

	public static ParentLoginResponse of(Long id, String name, String email, String image, Role role, String accessToken, String refreshToken) {
		return new ParentLoginResponse(id, name, email, image, role, accessToken, refreshToken);
	}
}
