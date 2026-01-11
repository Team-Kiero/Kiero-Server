package com.kiero.feed.presentation.dto;

import java.util.List;

public record FeedGetResponse(
	String childName,
	List<FeedItemDto> feedItems,
	String nextCursor
) {
}
