package com.kiero.global.notification.application.port.out;

import com.kiero.global.notification.domain.PushNotificationPayload;

public interface NotificationPublisherPort {
	void publish(PushNotificationPayload payload);
}