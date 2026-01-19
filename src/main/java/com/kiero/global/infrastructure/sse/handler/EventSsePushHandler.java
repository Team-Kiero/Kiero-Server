package com.kiero.global.infrastructure.sse.handler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.feed.infrastructure.event.dto.FeedItemsCreatedEvent;
import com.kiero.feed.repository.FeedItemRepository;
import com.kiero.global.infrastructure.sse.domain.SseEventType;
import com.kiero.global.infrastructure.sse.dto.SsePayload;
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

	private final FeedItemRepository feedItemRepository;
	private final EventSseService eventSseService;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FeedItemsCreatedEvent event) {
		List<FeedItem> items = feedItemRepository.findAllById(event.feedItemIds());

		for (FeedItem fi : items) {
			SseEventType sseEventType = mapToSseEventType(fi.getEventType());
			SsePayload payload = SsePayload.of(
				sseEventType,
				fi.getChild().getId(),
				fi.getOccurredAt(),
				fi.getMetadata()
			);

			Long parentId = fi.getParent().getId();
			log.debug("부모 SSE 푸시 (피드): parentId={}, childId={}, eventType={}",
				parentId, fi.getChild().getId(), sseEventType);

			eventSseService.pushToParent(parentId, payload);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ChildJoinedEvent event) {
		ObjectNode data = objectMapper.createObjectNode();
		data.put("childId", event.childId());
		data.put("childName", event.childName());

		SsePayload payload = SsePayload.of(
			SseEventType.CHILD_JOINED,
			event.childId(),
			event.occurredAt(),
			data
		);

		log.debug("부모 SSE 푸시 (자녀 가입): parentId={}, childId={}, childName={}",
			event.parentId(), event.childId(), event.childName());

		eventSseService.pushToParent(event.parentId(), payload);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(MissionCreatedEvent event) {
		ObjectNode data = objectMapper.createObjectNode();
		data.put("missionName", event.missionName());
		data.put("reward", event.reward());

		SsePayload payload = SsePayload.of(
			SseEventType.MISSION_CREATED,
			event.childId(),
			event.occurredAt(),
			data
		);

		log.debug("자녀 SSE 푸시 (미션 생성): childId={}, missionName={}",
			event.childId(), event.missionName());

		eventSseService.pushToChild(event.childId(), payload);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ScheduleCreatedEvent event) {
		ObjectNode data = objectMapper.createObjectNode();
		data.put("scheduleName", event.scheduleName());

		SsePayload payload = SsePayload.of(
			SseEventType.SCHEDULE_CREATED,
			event.childId(),
			event.occurredAt(),
			data
		);

		log.debug("자녀 SSE 푸시 (스케줄 생성): childId={}, scheduleName={}",
			event.childId(), event.scheduleName());

		eventSseService.pushToChild(event.childId(), payload);
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
