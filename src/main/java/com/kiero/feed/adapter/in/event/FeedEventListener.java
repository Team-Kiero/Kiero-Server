package com.kiero.feed.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiero.coupon.application.dto.CouponPurchaseEvent;
import com.kiero.feed.application.port.in.FeedCreateUseCase;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.mission.application.dto.MissionCompleteEvent;
import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.NowScheduleCompleteEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedEventListener {

	private final FeedCreateUseCase feedCreateUseCase;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(NowScheduleCompleteEvent event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("imageUrl", event.imageUrl());

		feedCreateUseCase.createForParentsOfChild(
			new FeedCreateUseCase.CreateFeedCommand(
				event.parents(),
				event.childId(),
				event.occurredAt(),
				EventType.SCHEDULE,
				metadata
			)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(FireLitEvent event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("amount", event.amount());

		feedCreateUseCase.createForParentsOfChild(
			new FeedCreateUseCase.CreateFeedCommand(
				event.parents(),
				event.childId(),
				event.occurredAt(),
				EventType.COMPLETE,
				metadata
			)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(MissionCompleteEvent event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("amount", event.amount());

		feedCreateUseCase.createForParentsOfChild(
			new FeedCreateUseCase.CreateFeedCommand(
				event.parents(),
				event.childId(),
				event.occurredAt(),
				EventType.MISSION,
				metadata
			)
		);
	}

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(CouponPurchaseEvent event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("content", event.name());
		metadata.put("amount", event.amount());

		feedCreateUseCase.createForParentsOfChild(
			new FeedCreateUseCase.CreateFeedCommand(
				event.parents(),
				event.childId(),
				event.occurredAt(),
				EventType.COUPON,
				metadata
			)
		);
	}
}