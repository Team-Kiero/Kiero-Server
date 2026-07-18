package com.kiero.feed.application.port.in;

public interface FeedReadUseCase {
	void markAsRead(Long parentId, Long scheduleDetailId);
}
