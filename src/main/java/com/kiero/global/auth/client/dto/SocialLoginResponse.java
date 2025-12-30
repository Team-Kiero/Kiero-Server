package com.kiero.global.auth.client.dto;

import com.kiero.global.auth.client.enums.Provider;

public record SocialLoginResponse(
	String socialId,
	Provider provider,
	String name,
	String email,
	String image
) {
	public static SocialLoginResponse of(
		String socialId,
		Provider provider,
		String name,
		String email,
		String image
	) {
		return new SocialLoginResponse(
			socialId,
			provider,
			name,
			email,
			image
		);
	}
}
