package com.kiero.feed.application.port.out;

public interface FeedItemTransferPort {
	void transferOwnership(Long fromParentId, Long toParentId, Long childId);
}
