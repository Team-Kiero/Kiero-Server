package com.kiero.feed.infrastructure.event.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;

public record FeedItemsCreatedEvent(
	List<FeedItemInfo> items
) {
	public record FeedItemInfo(
		Long feedItemId,
		Long parentId,
		Long childId,
		EventType eventType,
		LocalDateTime occurredAt,
		JsonNode metadata
	) {
	}
}
