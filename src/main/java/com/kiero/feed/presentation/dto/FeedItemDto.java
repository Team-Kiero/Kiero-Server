package com.kiero.feed.presentation.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.kiero.feed.domain.enums.EventType;

public record FeedItemDto(
	EventType eventType,
	@JsonFormat(pattern = "yyyy.MM.dd HH:mm")
	LocalDateTime occurredAt,
	JsonNode metadata
) {}
