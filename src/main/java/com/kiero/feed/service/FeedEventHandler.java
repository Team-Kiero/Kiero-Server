package com.kiero.feed.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiero.child.domain.Child;
import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.domain.enums.EventType;
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

	private final FeedItemRepository feedItemRepository;
	private final ParentChildRepository parentChildRepository;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

		feedItemRepository.saveAll(feedItems);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

		feedItemRepository.saveAll(feedItems);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

		feedItemRepository.saveAll(feedItems);
	}

}
