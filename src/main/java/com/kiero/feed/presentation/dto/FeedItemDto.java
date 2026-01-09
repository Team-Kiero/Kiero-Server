package com.kiero.feed.presentation.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;

public record FeedItemDto(
	EventType eventType,
	LocalDateTime createdAt,
	JsonNode metadata
) {}
