package com.kiero.global.sse.adapter.in.event;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kiero.child.application.dto.ChildJoinedEvent;
import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.feed.infrastructure.dto.FeedItemsCreatedEvent;
import com.kiero.global.sse.application.port.in.SsePushUseCase;
import com.kiero.global.sse.domain.SseEventType;
import com.kiero.mission.application.dto.MissionCreatedEvent;
import com.kiero.schedule.application.dto.ScheduleModifiedEvent;
import com.kiero.schedule.application.dto.ScheduleStatusUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsePushEventListener {

	private final SsePushUseCase ssePushUseCase;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FeedItemsCreatedEvent event) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("eventType", SseEventType.FEED_ITEM_CREATED.name());
			data.put("childId", event.childId());

		for (Long parentId : event.parentIds()) {
			log.debug("부모 SSE 푸시 (피드): parentId={}, childId={}", parentId, event.childId());
			ssePushUseCase.pushToParent(parentId, SseEventType.FEED_ITEM_CREATED, data);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ChildJoinedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.CHILD_JOINED.name());
		data.put("childId", event.childId());

		log.debug("부모 SSE 푸시 (자녀 가입): parentId={}, childId={}",
			event.parentId(), event.childId());

		ssePushUseCase.pushToParent(event.parentId(), SseEventType.CHILD_JOINED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(MissionCreatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.MISSION_CREATED.name());
		data.put("missionName", event.missionName());
		data.put("reward", event.reward());

		log.debug("자녀 SSE 푸시 (미션 생성): childId={}, missionName={}",
			event.childId(), event.missionName());

		ssePushUseCase.pushToChild(event.childId(), SseEventType.MISSION_CREATED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleModifiedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.SCHEDULE_MODIFIED.name());

		log.debug("자녀 SSE 푸시 (스케줄 알림): childId={}", event.childId());

		ssePushUseCase.pushToChild(event.childId(), SseEventType.SCHEDULE_MODIFIED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(CouponCreatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.COUPON_CREATED.name());
		data.put("couponName", event.couponName());
		data.put("price", event.price());

		log.debug("자녀 SSE 푸시 (쿠폰 생성): childId={}, couponName={}",
			event.childId(), event.couponName());

		ssePushUseCase.pushToChild(event.childId(), SseEventType.COUPON_CREATED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handler(ScheduleStatusUpdatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.SCHEDULE_STATUS_UPDATED.name());
		data.put("childId", event.childId());

		log.debug("자녀 및 부모 SSE 푸시 (스케줄 상태 변경): childId={}", event.childId());

		ssePushUseCase.pushToChild(event.childId(), SseEventType.SCHEDULE_STATUS_UPDATED, data);

		for (Long parentId : event.parentIds()) {
			ssePushUseCase.pushToParent(parentId, SseEventType.SCHEDULE_STATUS_UPDATED, data);
		}
	}
}