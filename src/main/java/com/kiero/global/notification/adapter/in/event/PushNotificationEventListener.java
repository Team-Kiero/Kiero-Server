package com.kiero.global.notification.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kiero.coupon.application.dto.CouponPurchasedEvent;
import com.kiero.global.notification.application.port.in.PushNotificationUseCase;
import com.kiero.global.notification.application.port.out.FeedItemLookupPort;
import com.kiero.global.notification.domain.PushNotificationType;
import com.kiero.mission.application.dto.MissionCompleteEvent;
import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.ScheduleCreatedEvent;
import com.kiero.schedule.application.dto.ScheduleDeletedEvent;
import com.kiero.schedule.application.dto.ScheduleModifiedPushEvent;
import com.kiero.schedule.application.dto.ScheduleSkippedEvent;
import com.kiero.schedule.application.dto.ScheduleVerifiedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationEventListener {

	private final PushNotificationUseCase pushNotificationUseCase;
	private final FeedItemLookupPort feedItemLookupPort;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleVerifiedEvent event) {
		for (Long parentId : event.parentIds()) {
			String feedItemId = feedItemLookupPort.findFeedItemIdByParentAndScheduleDetail(parentId, event.scheduleDetailId())
				.map(String::valueOf)
				.orElseGet(() -> {
					log.warn("feedItemId 조회 실패: type=SCHEDULE_VERIFIED, parentId={}, scheduleDetailId={}", parentId, event.scheduleDetailId());
					return "";
				});
			log.info("푸시 알림 (일정 인증): parentId={}, childId={}, feedItemId={}", parentId, event.childId(), feedItemId);
			pushNotificationUseCase.pushToParent(parentId, event.childId(), PushNotificationType.SCHEDULE_VERIFIED, feedItemId, event.scheduleName());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FireLitEvent event) {
		for (Long parentId : event.parentIds()) {
			String feedItemId = feedItemLookupPort.findFeedItemIdByParentAndChildComplete(parentId, event.childId(), event.occurredDate())
				.map(String::valueOf)
				.orElseGet(() -> {
					log.warn("feedItemId 조회 실패: type=FIRE_LIT, parentId={}, childId={}, date={}", parentId, event.childId(), event.occurredDate());
					return "";
				});
			log.info("푸시 알림 (불꽃 피우기): parentId={}, childId={}, feedItemId={}", parentId, event.childId(), feedItemId);
			pushNotificationUseCase.pushToParent(parentId, event.childId(), PushNotificationType.FIRE_LIT, feedItemId, "");
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(MissionCompleteEvent event) {
		for (Long parentId : event.parentIds()) {
			String feedItemId = feedItemLookupPort.findFeedItemIdByParentAndMission(parentId, event.missionId())
				.map(String::valueOf)
				.orElseGet(() -> {
					log.warn("feedItemId 조회 실패: type=MISSION_COMPLETE, parentId={}, missionId={}", parentId, event.missionId());
					return "";
				});
			log.info("푸시 알림 (미션 완료): parentId={}, childId={}, feedItemId={}", parentId, event.childId(), feedItemId);
			pushNotificationUseCase.pushToParent(parentId, event.childId(), PushNotificationType.MISSION_COMPLETE, feedItemId, event.missionName());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(CouponPurchasedEvent event) {
		for (Long parentId : event.parentIds()) {
			String feedItemId = feedItemLookupPort.findFeedItemIdByParentAndCoupon(parentId, event.couponId())
				.map(String::valueOf)
				.orElseGet(() -> {
					log.warn("feedItemId 조회 실패: type=COUPON_PURCHASED, parentId={}, couponId={}", parentId, event.couponId());
					return "";
				});
			log.info("푸시 알림 (쿠폰 구매): parentId={}, childId={}, feedItemId={}", parentId, event.childId(), feedItemId);
			pushNotificationUseCase.pushToParent(parentId, event.childId(), PushNotificationType.COUPON_PURCHASED, feedItemId, event.couponName());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleSkippedEvent event) {
		for (Long parentId : event.parentIds()) {
			log.info("푸시 알림 (일정 스킵): parentId={}, childId={}", parentId, event.childId());
			pushNotificationUseCase.pushToParent(parentId, event.childId(), PushNotificationType.SCHEDULE_SKIPPED, "", event.scheduleName());
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleCreatedEvent event) {
		log.info("푸시 알림 (여정 추가): childId={}", event.childId());
		pushNotificationUseCase.pushToChild(event.childId(), PushNotificationType.SCHEDULE_CREATED, "");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleDeletedEvent event) {
		log.info("푸시 알림 (여정 삭제): childId={}", event.childId());
		pushNotificationUseCase.pushToChild(event.childId(), PushNotificationType.SCHEDULE_DELETED, "");
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleModifiedPushEvent event) {
		log.info("푸시 알림 (여정 변경): childId={}", event.childId());
		pushNotificationUseCase.pushToChild(event.childId(), PushNotificationType.SCHEDULE_MODIFIED, "");
	}
}
