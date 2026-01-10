package com.kiero.feed.infrastructure.event;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiero.child.domain.Child;
import com.kiero.coupon.presentation.dto.CouponPurchaseEvent;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.feed.infrastructure.sse.FeedSseService;
import com.kiero.feed.infrastructure.sse.dto.FeedPushPayload;
import com.kiero.feed.repository.FeedItemRepository;
import com.kiero.mission.presentation.dto.MissionCompleteEvent;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.schedule.presentation.dto.NowScheduleCompleteEvent;
import com.kiero.schedule.presentation.dto.FireLitEvent;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedEventHandler {

	@PersistenceContext
	private EntityManager entityManager;

	private final FeedSseService feedSseService;
	private final FeedItemRepository feedItemRepository;
	private final ParentChildRepository parentChildRepository;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(NowScheduleCompleteEvent event) {
		Child childRef = entityManager.getReference(Child.class, event.childId());
		List<Parent> parents = parentChildRepository.findParentsByChildId(event.childId());

		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("imageUrl", event.imageUrl());

		List<FeedItem> feedItems = parents.stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				event.occurredAt(),
				EventType.SCHEDULE,
				metadata.deepCopy()
			))
			.toList();

		List<FeedItem> saved = feedItemRepository.saveAll(feedItems);
		pushSseEvent(saved);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(FireLitEvent event) {
		Child childRef = entityManager.getReference(Child.class, event.childId());
		List<Parent> parents = parentChildRepository.findParentsByChildId(event.childId());

		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("amount", event.amount());

		List<FeedItem> feedItems = parents.stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				event.occurredAt(),
				EventType.COMPLETE,
				metadata.deepCopy()
			))
			.toList();

		List<FeedItem> saved = feedItemRepository.saveAll(feedItems);
		pushSseEvent(saved);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(MissionCompleteEvent event) {
		Child childRef = entityManager.getReference(Child.class, event.childId());
		List<Parent> parents = parentChildRepository.findParentsByChildId(event.childId());

		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("amount", event.amount());

		List<FeedItem> feedItems = parents.stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				event.occurredAt(),
				EventType.MISSION,
				metadata.deepCopy()
			))
			.toList();

		List<FeedItem> saved = feedItemRepository.saveAll(feedItems);
		pushSseEvent(saved);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(CouponPurchaseEvent event) {
		Child childRef = entityManager.getReference(Child.class, event.childId());
		List<Parent> parents = parentChildRepository.findParentsByChildId(event.childId());

		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("amount", event.amount());

		List<FeedItem> feedItems = parents.stream()
			.map(parent -> FeedItem.create(
				parent,
				childRef,
				event.occurredAt(),
				EventType.COUPON,
				metadata.deepCopy()
			))
			.toList();

		List<FeedItem> saved = feedItemRepository.saveAll(feedItems);
		pushSseEvent(saved);
	}

	private void pushSseEvent(List<FeedItem> feedItems) {
		for (FeedItem fi : feedItems) {
			feedSseService.push(
				fi.getParent().getId(),
				fi.getChild().getId(),
				new FeedPushPayload(
					fi.getId(),
					fi.getEventType(),
					fi.getOccurredAt(),
					fi.getMetadata()
				)
			);
		}
	}

}
