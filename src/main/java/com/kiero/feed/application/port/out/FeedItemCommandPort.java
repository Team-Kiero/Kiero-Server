package com.kiero.feed.application.port.out;

import java.util.List;

import com.kiero.feed.domain.FeedItem;

public interface FeedItemCommandPort {
	List<FeedItem> saveAll(List<FeedItem> items);
	void markAllAsRead(List<Long> itemIds);
}
