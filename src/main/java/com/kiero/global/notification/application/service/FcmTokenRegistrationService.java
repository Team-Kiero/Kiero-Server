package com.kiero.global.notification.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.application.port.out.ChildSavePort;
import com.kiero.child.domain.Child;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.global.notification.application.dto.NotificationSettingsResponse;
import com.kiero.global.notification.application.port.in.FcmTokenRegistrationUseCase;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FcmTokenRegistrationService implements FcmTokenRegistrationUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ParentSavePort parentSavePort;
	private final ChildLoadPort childLoadPort;
	private final ChildSavePort childSavePort;

	@Override
	@Transactional
	public void registerFcmToken(Long memberId, Role role, String fcmToken) {
		if (role == Role.PARENT) {
			Parent parent = parentLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
			parent.updateFcmToken(fcmToken);
			parentSavePort.save(parent);
		} else {
			Child child = childLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));
			child.updateFcmToken(fcmToken);
			childSavePort.save(child);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public NotificationSettingsResponse getNotificationSettings(Long memberId, Role role) {
		if (role == Role.PARENT) {
			Parent parent = parentLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
			return new NotificationSettingsResponse(parent.isPushNotificationEnabled());
		} else {
			Child child = childLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));
			return new NotificationSettingsResponse(child.isPushNotificationEnabled());
		}
	}

	@Override
	@Transactional
	public void updateNotificationSettings(Long memberId, Role role, boolean enabled) {
		if (role == Role.PARENT) {
			Parent parent = parentLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
			parent.updatePushNotificationEnabled(enabled);
			parentSavePort.save(parent);
		} else {
			Child child = childLoadPort.findById(memberId)
				.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));
			child.updatePushNotificationEnabled(enabled);
			childSavePort.save(child);
		}
	}
}
