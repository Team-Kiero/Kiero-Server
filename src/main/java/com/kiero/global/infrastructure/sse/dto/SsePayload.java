package com.kiero.global.infrastructure.sse.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.global.infrastructure.sse.domain.SseEventType;

public record SsePayload(
	SseEventType eventType,
	Long childId,
	LocalDateTime occurredAt,
	JsonNode data
) {

	public static SsePayload of(SseEventType eventType, Long childId, LocalDateTime occurredAt, JsonNode data) {
		return new SsePayload(eventType, childId, occurredAt, data);
	}

	public static SsePayload of(SseEventType eventType, Long childId, JsonNode data) {
		return new SsePayload(eventType, childId, LocalDateTime.now(), data);
	}

	public static SsePayload of(SseEventType eventType, JsonNode data) {
		return new SsePayload(eventType, null, LocalDateTime.now(), data);
	}
}
