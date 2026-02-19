package com.kiero.feeds.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.kiero.feeds.application.port.out.FeedItemCommandPort;
import com.kiero.feeds.application.port.out.FeedItemQueryPort;
import com.kiero.feeds.domain.FeedItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedItemPersistenceAdapter implements FeedItemCommandPort, FeedItemQueryPort {

	private final FeedItemRepository feedItemRepository;

	@Override
	public List<FeedItem> saveAll(List<FeedItem> feedItems) {
		return feedItemRepository.saveAll(feedItems);
	}

	@Override
	public List<FeedItem> findByCursor(
		Long parentId,
		Long childId,
		LocalDateTime cursorOccurredAt,
		Long cursorId,
		Pageable pageable
	) {
		return feedItemRepository.findFeedItemsByCursor(
			parentId,
			childId,
			cursorOccurredAt,
			cursorId,
			pageable
		);
	}
}
