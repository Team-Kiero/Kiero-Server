package com.kiero.feed.presentation.dto;

import java.util.List;

public record FeedGetResponse(
	List<FeedItemDto> feedItems,
	String nextCursor
) {
}
