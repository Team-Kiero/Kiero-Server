package com.kiero.feed.application.dto;

import java.util.List;

public record FeedHasUnreadResponse(
	boolean hasUnread,
	List<Long> unreadChildIds
) {
}