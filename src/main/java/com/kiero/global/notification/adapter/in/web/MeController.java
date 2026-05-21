package com.kiero.global.notification.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.notification.adapter.in.web.dto.FcmTokenRegisterRequest;
import com.kiero.global.notification.adapter.in.web.dto.NotificationSettingsUpdateRequest;
import com.kiero.global.notification.application.dto.NotificationSettingsResponse;
import com.kiero.global.notification.application.exception.NotificationSuccessCode;
import com.kiero.global.notification.application.port.in.FcmTokenRegistrationUseCase;
import com.kiero.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/common/push")
public class MeController {

	private final FcmTokenRegistrationUseCase fcmTokenRegistrationUseCase;

	@PreAuthorize("hasAnyRole('PARENT', 'CHILD', 'ADMIN')")
	@PatchMapping("/fcm-token")
	public ResponseEntity<SuccessResponse<Void>> registerFcmToken(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody FcmTokenRegisterRequest request
	) {
		fcmTokenRegistrationUseCase.registerFcmToken(
			currentAuth.memberId(), currentAuth.role(), request.fcmToken()
		);
		return ResponseEntity.ok(SuccessResponse.of(NotificationSuccessCode.FCM_TOKEN_REGISTERED));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'CHILD', 'ADMIN')")
	@GetMapping("/notification-settings")
	public ResponseEntity<SuccessResponse<NotificationSettingsResponse>> getNotificationSettings(
		@CurrentMember CurrentAuth currentAuth
	) {
		NotificationSettingsResponse response = fcmTokenRegistrationUseCase.getNotificationSettings(
			currentAuth.memberId(), currentAuth.role()
		);
		return ResponseEntity.ok(SuccessResponse.of(NotificationSuccessCode.NOTIFICATION_SETTINGS_FETCHED, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'CHILD', 'ADMIN')")
	@PatchMapping("/notification-settings")
	public ResponseEntity<SuccessResponse<Void>> updateNotificationSettings(
		@CurrentMember CurrentAuth currentAuth,
		@RequestBody NotificationSettingsUpdateRequest request
	) {
		fcmTokenRegistrationUseCase.updateNotificationSettings(
			currentAuth.memberId(), currentAuth.role(), request.pushNotificationEnabled()
		);
		return ResponseEntity.ok(SuccessResponse.of(NotificationSuccessCode.NOTIFICATION_SETTINGS_UPDATED));
	}
}
