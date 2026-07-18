package com.kiero.feed.infrastructure.dto;

import java.util.List;

public record FeedItemsCreatedEvent(
	Long childId,
	List<Long> parentIds
) {
}
