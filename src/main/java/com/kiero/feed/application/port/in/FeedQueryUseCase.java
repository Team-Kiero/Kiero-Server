package com.kiero.feed.application.port.in;

import com.kiero.feed.application.dto.FeedGetResponse;
import com.kiero.feed.application.dto.FeedHasUnreadResponse;

public interface FeedQueryUseCase {
	FeedGetResponse getFeedAndMarkAsRead(Long parentId, Long childId, Integer size, String cursor);
	FeedHasUnreadResponse getHasUnread(Long parentId);
}
