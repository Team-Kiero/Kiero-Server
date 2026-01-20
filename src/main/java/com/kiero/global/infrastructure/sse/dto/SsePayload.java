package com.kiero.global.infrastructure.sse.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.global.infrastructure.sse.domain.SseEventType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SsePayload(
	SseEventType eventType,
	Long feedItemId,
	Long childId,
	LocalDateTime occurredAt,
	JsonNode metadata
) {

	// 피드 이벤트용 (feedItemId 포함)
	public static SsePayload ofFeed(SseEventType eventType, Long feedItemId, Long childId, LocalDateTime occurredAt, JsonNode metadata) {
		return new SsePayload(eventType, feedItemId, childId, occurredAt, metadata);
	}

	// 비피드 이벤트용 (feedItemId 없음)
	public static SsePayload of(SseEventType eventType, Long childId, LocalDateTime occurredAt, JsonNode metadata) {
		return new SsePayload(eventType, null, childId, occurredAt, metadata);
	}

	public static SsePayload of(SseEventType eventType, Long childId, JsonNode metadata) {
		return new SsePayload(eventType, null, childId, LocalDateTime.now(), metadata);
	}
}
