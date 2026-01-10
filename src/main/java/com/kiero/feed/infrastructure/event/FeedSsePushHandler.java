package com.kiero.feed.infrastructure.event;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.infrastructure.event.dto.FeedItemsCreatedEvent;
import com.kiero.feed.repository.FeedItemRepository;
import com.kiero.feed.service.FeedSseService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedSsePushHandler {

	private final FeedItemRepository feedItemRepository;
	private final FeedSseService feedSseService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(FeedItemsCreatedEvent event) {
		List<FeedItem> items = feedItemRepository.findAllById(event.feedItemIds());

		for (FeedItem fi : items) {
			feedSseService.push(fi);
		}
	}
}
