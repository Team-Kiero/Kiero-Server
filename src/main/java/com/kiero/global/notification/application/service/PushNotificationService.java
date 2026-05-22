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
	public void pushToParent(Long parentId, Long childId, PushNotificationType type, String targetId, String targetName) {
		Parent parent = parentLoadPort.findById(parentId).orElse(null);
		if (parent == null || parent.getFcmToken() == null || parent.getFcmToken().isBlank() || !parent.isPushNotificationEnabled()) {
			log.debug("푸시 알림 스킵 (부모): parentId={}, type={}", parentId, type);
			return;
		}

		String childName = childLoadPort.findById(childId)
			.map(c -> c.getFirstName())
			.orElse("");

		String title = buildParentTitle(type, childName);
		String body = buildParentBody(type, childName, targetName);

		notificationPublisherPort.publish(new PushNotificationPayload(
			parent.getFcmToken(), title, body, type, targetId
		));
		log.debug("푸시 알림 발행 (부모): parentId={}, type={}", parentId, type);
	}

	@Override
	@Transactional(readOnly = true)
	public void pushToChild(Long childId, PushNotificationType type, String targetName) {
		Child child = childLoadPort.findById(childId).orElse(null);
		if (child == null || child.getFcmToken() == null || child.getFcmToken().isBlank() || !child.isPushNotificationEnabled()) {
			log.debug("푸시 알림 스킵 (자녀): childId={}, type={}", childId, type);
			return;
		}

		String title = buildChildTitle(type);
		String body = buildChildBody(type, targetName);

		notificationPublisherPort.publish(new PushNotificationPayload(
			child.getFcmToken(), title, body, type, ""
		));
		log.debug("푸시 알림 발행 (자녀): childId={}, type={}", childId, type);
	}

	private String buildParentTitle(PushNotificationType type, String childName) {
		return switch (type) {
			case PARENT_DAILY_START -> "하루 시작 ☀️";
			case SCHEDULE_VERIFIED -> "일정 인증 완료 📷";
			case FIRE_LIT -> "하루 일정 전체 완료 🔥";
			case MISSION_COMPLETE -> "미션 완료 🔥";
			case COUPON_PURCHASED -> "쿠폰 사용 🔥";
			case SCHEDULE_SKIPPED -> "일정 스킵 안내 💡";
			case PARENT_SCHEDULE_REMINDER -> "일정 확인 필요 🕒";
			default -> childName + "에게서 알림이 왔어요!";
		};
	}

	private String buildParentBody(PushNotificationType type, String childName, String targetName) {
		String safeTargetName = targetName == null ? "" : targetName;
		String nameWithParticle = appendParticle(childName);
		return switch (type) {
			case PARENT_DAILY_START -> appendPossessive(childName) + " 하루가 시작됐어요!\n오늘의 일정과 미션을 확인해볼까요?";
			case SCHEDULE_VERIFIED -> nameWithParticle + " '" + safeTargetName + "'에 도착했어요!\n인증사진을 확인해볼까요?";
			case FIRE_LIT -> nameWithParticle + " 오늘 일정을 모두 완료했어요!\n하루를 잘 마무리했는지 확인해볼까요?";
			case MISSION_COMPLETE -> nameWithParticle + " '" + safeTargetName + "' 미션을 완료했어요.\n보상 금화가 지급됐어요!";
			case COUPON_PURCHASED -> nameWithParticle + " '" + safeTargetName + "' 쿠폰을 사용했어요.\n사용한 보상을 확인해볼까요?";
			case SCHEDULE_SKIPPED -> nameWithParticle + " '" + safeTargetName + "' 일정을 건너뛰었어요.\n오늘 여정을 확인해볼까요?";
			case PARENT_SCHEDULE_REMINDER -> "'" + safeTargetName + "' 시간이 지났지만 아직 인증이 없어요.\n" + appendPossessive(childName) + " 상태를 확인해볼까요?";
			default -> nameWithParticle + " 활동했어요!";
		};
	}

	private String buildChildTitle(PushNotificationType type) {
		return switch (type) {
			case CHILD_DAILY_START -> "오늘의 여정 시작! ☀️";
			case CHILD_NEXT_JOURNEY -> "다음 여정이 시작돼! 🔥";
			case CHILD_MISSION_INCOMPLETE -> "미션이 남았어!";
			case SCHEDULE_CREATED -> "여정이 추가됐어! 🔥";
			case SCHEDULE_DELETED -> "취소된 여정이 있어!";
			case SCHEDULE_MODIFIED -> "변경된 여정이 있어!";
			default -> "새로운 알림이 왔어요!";
		};
	}

	private String buildChildBody(PushNotificationType type, String targetName) {
		String safeTargetName = targetName == null ? "" : targetName;
		return switch (type) {
			case CHILD_DAILY_START -> "오늘 할 일이 기다리고 있어. 여정을 시작하자!";
			case CHILD_NEXT_JOURNEY -> "'" + safeTargetName + "' 갈 시간이야. 도착하면 불조각을 모아줘!";
			case CHILD_MISSION_INCOMPLETE -> "아직 끝내지 않은 미션이 있어. 완료하고 금화를 받자!";
			case SCHEDULE_CREATED -> "새롭게 추가된 여정이 있어! 확인하고 불조각을 모아줘!";
			case SCHEDULE_DELETED -> "오늘 취소된 여정이 있어. 확인해줘!";
			case SCHEDULE_MODIFIED -> "오늘 변경된 여정이 있어. 확인해줘!";
			default -> "확인해봐요!";
		};
	}

	private String appendPossessive(String name) {
		if (name == null || name.isEmpty()) {
			return name;
		}
		char last = name.charAt(name.length() - 1);
		if (last >= 0xAC00 && last <= 0xD7A3) {
			boolean hasBatchim = (last - 0xAC00) % 28 != 0;
			return name + (hasBatchim ? "이의" : "의");
		}
		return name + "의";
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
