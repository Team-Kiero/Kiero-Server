package com.kiero.feed.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.feed.application.port.in.FeedCreateUseCase;
import com.kiero.feed.application.port.out.FeedItemCommandPort;
import com.kiero.feed.application.port.out.ParentChildQueryPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.infrastructure.dto.FeedItemsCreatedEvent;
import com.kiero.feed.infrastructure.dto.FeedItemsCreatedEvent.FeedItemInfo;
import com.kiero.parent.domain.Parent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCreateService implements FeedCreateUseCase {

	@PersistenceContext
	private final EntityManager entityManager;

	private final FeedItemCommandPort feedItemCommandPort;
	private final ParentChildQueryPort parentChildQueryPort;
	private final ApplicationEventPublisher publisher;

	@Override
	@Transactional
	public void createForParentsOfChild(CreateFeedCommand command) {

		Child childRef = entityManager.getReference(Child.class, command.childId());
		List<Parent> parents = parentChildQueryPort.findParentsByChildId(command.childId());

		List<FeedItem> feedItems = parents.stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				command.occurredAt(),
				command.eventType(),
				command.metadata().deepCopy()
			))
			.toList();

		List<FeedItem> saved = feedItemCommandPort.saveAll(feedItems);

		List<FeedItemInfo> items = saved.stream()
			.map(fi -> new FeedItemInfo(
				fi.getId(),
				fi.getParent().getId(),
				fi.getChild().getId(),
				fi.getEventType(),
				fi.getOccurredAt(),
				fi.getMetadata()
			))
			.toList();

		publisher.publishEvent(new FeedItemsCreatedEvent(items));
	}
}