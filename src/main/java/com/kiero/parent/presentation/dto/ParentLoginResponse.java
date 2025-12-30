package com.kiero.parent.presentation.dto;

import com.kiero.global.auth.enums.Role;

public record ParentLoginResponse(
	String name,
	String email,
	String image,
	Role role,
	String accessToken,
	String refreshToken
) {

	public static ParentLoginResponse of(String name, String email, String image, Role role, String accessToken, String refreshToken) {
		return new ParentLoginResponse(name, email, image, role, accessToken, refreshToken);
	}
}
