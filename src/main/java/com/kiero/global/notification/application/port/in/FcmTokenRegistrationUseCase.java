package com.kiero.global.notification.application.port.in;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.notification.application.dto.NotificationSettingsResponse;

public interface FcmTokenRegistrationUseCase {
	void registerFcmToken(Long memberId, Role role, String fcmToken);
	void clearFcmToken(Long memberId, Role role);
	NotificationSettingsResponse getNotificationSettings(Long memberId, Role role);
	void updateNotificationSettings(Long memberId, Role role, boolean enabled);
}
