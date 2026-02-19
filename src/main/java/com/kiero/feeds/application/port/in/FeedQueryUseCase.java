package com.kiero.feeds.application.port.in;

import com.kiero.feeds.application.dto.FeedGetResponse;

public interface FeedQueryUseCase {
	FeedGetResponse getFeed(Long parentId, Long childId, Integer size, String cursor);
}
