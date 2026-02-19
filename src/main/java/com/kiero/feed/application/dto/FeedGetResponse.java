package com.kiero.feed.application.dto;

import java.util.List;

public record FeedGetResponse(
	String childName,
	List<FeedItemDto> feedItems,
	String nextCursor
) {
}
