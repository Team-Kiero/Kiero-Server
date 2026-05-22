package com.kiero.feed.application.port.out;

public interface FeedItemDeletePort {
	void deleteAllByChildId(Long childId);
}
