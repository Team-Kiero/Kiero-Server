package com.kiero.feed.infrastructure.sse.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;

public record FeedPushPayload(
	Long feedItemId,
	EventType eventType,
	LocalDateTime occurredAt,
	JsonNode metadata
) {}
