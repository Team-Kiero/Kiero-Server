package com.kiero.feed.application.port.in;

import com.kiero.feed.application.dto.FeedGetResponse;

public interface FeedQueryUseCase {
	FeedGetResponse getFeed(Long parentId, Long childId, Integer size, String cursor);
}
