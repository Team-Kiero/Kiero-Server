package com.kiero.feed.adapter.in.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kiero.coupon.application.dto.CouponPurchaseEventForFeed;
import com.kiero.feed.application.port.in.FeedCreateUseCase;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.mission.application.dto.MissionCompleteEventForFeed;
import com.kiero.schedule.application.dto.FireLitEventForFeed;
import com.kiero.schedule.application.dto.NowScheduleCompleteEventForFeed;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedEventListener {

	private final FeedCreateUseCase feedCreateUseCase;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void handle(NowScheduleCompleteEventForFeed event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("scheduleDetailId", event.scheduleDetailId());
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
	public void handle(FireLitEventForFeed event) {
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
	public void handle(MissionCompleteEventForFeed event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("missionId", event.missionId());
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
	public void handle(CouponPurchaseEventForFeed event) {
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("couponId", event.couponId());
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