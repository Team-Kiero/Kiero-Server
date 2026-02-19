package com.kiero.feeds.application.port.out;

import java.util.List;

import com.kiero.feeds.domain.FeedItem;

public interface FeedItemCommandPort {
	List<FeedItem> saveAll(List<FeedItem> items);
}
