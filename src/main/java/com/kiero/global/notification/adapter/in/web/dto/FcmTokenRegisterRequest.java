package com.kiero.global.notification.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRegisterRequest(
	@NotBlank String fcmToken
) {
}
