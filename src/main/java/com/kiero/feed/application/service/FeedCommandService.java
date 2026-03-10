package com.kiero.feed.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.feed.application.exception.FeedErrorCode;
import com.kiero.feed.application.port.in.FeedCreateUseCase;
import com.kiero.feed.application.port.in.FeedReadUseCase;
import com.kiero.feed.application.port.out.FeedItemCommandPort;
import com.kiero.feed.application.port.out.FeedItemQueryPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.feed.infrastructure.dto.FeedItemsCreatedEvent;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCommandService implements FeedCreateUseCase, FeedReadUseCase {

	@PersistenceContext
	private final EntityManager entityManager;

	private final FeedItemCommandPort feedItemCommandPort;
	private final ApplicationEventPublisher publisher;
	private final FeedItemQueryPort feedItemQueryPort;

	@Override
	@Transactional
	public void createForParentsOfChild(CreateFeedCommand command) {

		Child childRef = entityManager.getReference(Child.class, command.childId());

		List<FeedItem> feedItems = command.parents().stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				command.occurredAt(),
				command.eventType(),
				command.metadata().deepCopy()
			))
			.toList();

		feedItemCommandPort.saveAll(feedItems);

		List<Long> parentIds = command.parents().stream()
			.map(Parent::getId)
			.toList();

		publisher.publishEvent(new FeedItemsCreatedEvent(childRef.getId(), parentIds));
	}

	@Override
	@Transactional
	public void markAsRead(Long parentId, Long scheduleDetailId) {
		FeedItem feedItem = feedItemQueryPort.findByParentIdAndScheduleDetailIdAndEventType(parentId, scheduleDetailId,
			EventType.SCHEDULE)
			.orElseThrow(() -> new KieroException(FeedErrorCode.FEED_ITEM_NOT_FOUND));

		feedItem.markAsRead();
	}

	@Transactional
	protected void markAllAsRead(List<Long> unreadItemIds) {
		if (unreadItemIds == null || unreadItemIds.isEmpty()) return;
		feedItemCommandPort.markAllAsRead(unreadItemIds);
	}
}