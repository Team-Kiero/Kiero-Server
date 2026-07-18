package com.kiero.global.notification.domain;

public record PushNotificationPayload(
	String fcmToken,
	String title,
	String body,
	PushNotificationType type,
	String targetId
) {
}