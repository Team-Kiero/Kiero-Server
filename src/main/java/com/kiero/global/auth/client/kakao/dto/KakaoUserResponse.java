package com.kiero.global.auth.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoUserResponse(
	Long id,
	KakaoAccount kakaoAccount
) {
	public record KakaoAccount(
		Profile profile,
		String email
	) {}

	public record Profile(
		String nickname,
		@JsonProperty("profile_image_url") String profileImageUrl,
		@JsonProperty("thumbnail_image_url") String thumbnailImageUrl,
		@JsonProperty("is_default_image") Boolean isDefaultImage
	) {}
}
