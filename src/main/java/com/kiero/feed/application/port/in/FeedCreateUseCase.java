package com.kiero.feed.application.port.in;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;

public interface FeedCreateUseCase {

	void createForParentsOfChild(CreateFeedCommand command);

	record CreateFeedCommand(
		Long childId,
		LocalDateTime occurredAt,
		EventType eventType,
		JsonNode metadata
	) {}
}