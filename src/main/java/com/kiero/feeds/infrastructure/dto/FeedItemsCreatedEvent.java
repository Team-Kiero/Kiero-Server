package com.kiero.feeds.infrastructure.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feeds.domain.enums.EventType;

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
