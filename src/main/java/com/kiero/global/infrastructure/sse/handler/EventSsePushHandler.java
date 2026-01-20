package com.kiero.global.infrastructure.sse.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kiero.feed.domain.enums.EventType;
import com.kiero.feed.infrastructure.event.dto.FeedItemsCreatedEvent;
import com.kiero.feed.infrastructure.event.dto.FeedItemsCreatedEvent.FeedItemInfo;
import com.kiero.global.infrastructure.sse.domain.SseEventType;
import com.kiero.child.presentation.dto.ChildJoinedEvent;
import com.kiero.mission.presentation.dto.MissionCreatedEvent;
import com.kiero.schedule.presentation.dto.ScheduleCreatedEvent;
import com.kiero.global.infrastructure.sse.service.EventSseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSsePushHandler {

	private final EventSseService eventSseService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FeedItemsCreatedEvent event) {
		for (FeedItemInfo item : event.items()) {
			SseEventType sseEventType = mapToSseEventType(item.eventType());

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("eventType", sseEventType.name());
			data.put("feedItemId", item.feedItemId());
			data.put("childId", item.childId());
			data.put("occurredAt", item.occurredAt().toString());
			data.put("metadata", item.metadata());

			log.debug("부모 SSE 푸시 (피드): parentId={}, childId={}, feedItemId={}, eventType={}",
				item.parentId(), item.childId(), item.feedItemId(), sseEventType);

			eventSseService.pushToParent(item.parentId(), sseEventType, data);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ChildJoinedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.CHILD_JOINED.name());
		data.put("childId", event.childId());

		log.debug("부모 SSE 푸시 (자녀 가입): parentId={}, childId={}",
			event.parentId(), event.childId());

		eventSseService.pushToParent(event.parentId(), SseEventType.CHILD_JOINED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(MissionCreatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.MISSION_CREATED.name());
		data.put("missionName", event.missionName());
		data.put("reward", event.reward());

		log.debug("자녀 SSE 푸시 (미션 생성): childId={}, missionName={}",
			event.childId(), event.missionName());

		eventSseService.pushToChild(event.childId(), SseEventType.MISSION_CREATED, data);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleCreatedEvent event) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("eventType", SseEventType.SCHEDULE_CREATED.name());
		data.put("scheduleName", event.scheduleName());

		log.debug("자녀 SSE 푸시 (스케줄 생성): childId={}, scheduleName={}",
			event.childId(), event.scheduleName());

		eventSseService.pushToChild(event.childId(), SseEventType.SCHEDULE_CREATED, data);
	}

	private SseEventType mapToSseEventType(EventType eventType) {
		return switch (eventType) {
			case MISSION -> SseEventType.MISSION_COMPLETED;
			case SCHEDULE -> SseEventType.SCHEDULE_COMPLETED;
			case COUPON -> SseEventType.COUPON_PURCHASED;
			case COMPLETE -> SseEventType.FIRE_LIT;
		};
	}
}
