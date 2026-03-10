package com.kiero.feed.application.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.feed.application.port.in.FeedCreateUseCase;
import com.kiero.feed.application.port.out.FeedItemCommandPort;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.infrastructure.dto.FeedItemsCreatedEvent;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.domain.Parent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedCommandService implements FeedCreateUseCase {

	@PersistenceContext
	private final EntityManager entityManager;

	private final ParentChildLoadPort parentChildLoadPort;

	private final FeedItemCommandPort feedItemCommandPort;
	private final ApplicationEventPublisher publisher;

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
}