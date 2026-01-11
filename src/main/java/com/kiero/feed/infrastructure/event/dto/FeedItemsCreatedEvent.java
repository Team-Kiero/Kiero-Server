package com.kiero.feed.infrastructure.event.dto;

import java.util.List;

public record FeedItemsCreatedEvent(
	List<Long> feedItemIds
) {
}
