package com.kiero.feed.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;
import com.kiero.parent.domain.Parent;

public interface FeedCreateUseCase {

	void createForParentsOfChild(CreateFeedCommand command);

	record CreateFeedCommand(
		List<Parent> parents,
		Long childId,
		LocalDateTime occurredAt,
		EventType eventType,
		JsonNode metadata
	) {}
}