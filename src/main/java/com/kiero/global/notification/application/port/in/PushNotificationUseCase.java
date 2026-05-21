package com.kiero.global.notification.application.port.in;

import com.kiero.global.notification.domain.PushNotificationType;

public interface PushNotificationUseCase {
	void pushToParent(Long parentId, PushNotificationType type, String childName, String targetId);
	void pushToChild(Long childId, PushNotificationType type);
}
