package com.kiero.global.notification.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.notification.application.port.in.PushNotificationUseCase;
import com.kiero.global.notification.application.port.out.NotificationPublisherPort;
import com.kiero.global.notification.domain.PushNotificationPayload;
import com.kiero.global.notification.domain.PushNotificationType;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService implements PushNotificationUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final NotificationPublisherPort notificationPublisherPort;

	@Override
	@Transactional(readOnly = true)
	public void pushToParent(Long parentId, Long childId, PushNotificationType type, String targetId) {
		Parent parent = parentLoadPort.findById(parentId).orElse(null);
		if (parent == null || parent.getFcmToken() == null || !parent.isPushNotificationEnabled()) {
			log.debug("푸시 알림 스킵 (부모): parentId={}, type={}", parentId, type);
			return;
		}

		String childName = childLoadPort.findById(childId)
			.map(c -> c.getFullName())
			.orElse("");

		String title = buildParentTitle(type, childName);
		String body = buildParentBody(type, childName);

		notificationPublisherPort.publish(new PushNotificationPayload(
			parent.getFcmToken(), title, body, type, targetId
		));
		log.debug("푸시 알림 발행 (부모): parentId={}, type={}", parentId, type);
	}

	@Override
	@Transactional(readOnly = true)
	public void pushToChild(Long childId, PushNotificationType type) {
		Child child = childLoadPort.findById(childId).orElse(null);
		if (child == null || child.getFcmToken() == null || !child.isPushNotificationEnabled()) {
			log.debug("푸시 알림 스킵 (자녀): childId={}, type={}", childId, type);
			return;
		}

		String title = buildChildTitle(type);
		String body = buildChildBody(type);

		notificationPublisherPort.publish(new PushNotificationPayload(
			child.getFcmToken(), title, body, type, ""
		));
		log.debug("푸시 알림 발행 (자녀): childId={}, type={}", childId, type);
	}

	private String buildParentTitle(PushNotificationType type, String childName) {
		return switch (type) {
			case PARENT_DAILY_START -> "☀️ " + childName + "의 하루 시작!";
			case SCHEDULE_VERIFIED -> "📷 일정 인증 완료!";
			case FIRE_LIT -> "🔥 하루 완료!";
			case MISSION_COMPLETE -> "🔥 미션 완료!";
			case COUPON_PURCHASED -> "🔥 쿠폰 구매!";
			case SCHEDULE_SKIPPED -> "💡 일정 스킵 안내";
			case PARENT_SCHEDULE_REMINDER -> "⏰ 일정 확인이 필요해요";
			default -> childName + "에게서 알림이 왔어요!";
		};
	}

	private String buildParentBody(PushNotificationType type, String childName) {
		String nameWithParticle = appendParticle(childName);
		return switch (type) {
			case PARENT_DAILY_START -> nameWithParticle + " 오늘의 여정을 시작했어요!";
			case SCHEDULE_VERIFIED -> nameWithParticle + " 일정을 인증했어요!";
			case FIRE_LIT -> nameWithParticle + " 오늘의 불꽃을 피웠어요!";
			case MISSION_COMPLETE -> nameWithParticle + " 미션을 완료했어요!";
			case COUPON_PURCHASED -> nameWithParticle + " 쿠폰을 사용했어요!";
			case SCHEDULE_SKIPPED -> nameWithParticle + " 일정을 스킵했어요.";
			case PARENT_SCHEDULE_REMINDER -> childName + "의 일정이 아직 완료되지 않았어요.";
			default -> nameWithParticle + " 활동했어요!";
		};
	}

	private String buildChildTitle(PushNotificationType type) {
		return switch (type) {
			case CHILD_DAILY_START -> "☀️ 오늘의 여정 시작!";
			case CHILD_NEXT_JOURNEY -> "🔥 다음 여정이 시작돼!";
			case CHILD_MISSION_INCOMPLETE -> "미션이 남았어!";
			case SCHEDULE_CREATED -> "🔥 여정이 추가됐어!";
			case SCHEDULE_DELETED -> "취소된 여정이 있어!";
			case SCHEDULE_MODIFIED -> "변경된 여정이 있어!";
			default -> "새로운 알림이 왔어요!";
		};
	}

	private String buildChildBody(PushNotificationType type) {
		return switch (type) {
			case CHILD_DAILY_START -> "오늘도 멋진 하루를 시작해봐요!";
			case CHILD_NEXT_JOURNEY -> "10분 후 여정이 시작돼요!";
			case CHILD_MISSION_INCOMPLETE -> "오늘의 미션이 아직 남아있어요. 지금 도전해봐요!";
			case SCHEDULE_CREATED -> "새로운 여정이 추가됐어요!";
			case SCHEDULE_DELETED -> "오늘의 여정이 취소됐어요.";
			case SCHEDULE_MODIFIED -> "오늘의 여정이 변경됐어요.";
			default -> "확인해봐요!";
		};
	}

	private String appendParticle(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		char last = name.charAt(name.length() - 1);
		if (last >= 0xAC00 && last <= 0xD7A3) {
			boolean hasBatchim = (last - 0xAC00) % 28 != 0;
			return name + (hasBatchim ? "이가" : "가");
		}
		return name + "가";
	}
}
