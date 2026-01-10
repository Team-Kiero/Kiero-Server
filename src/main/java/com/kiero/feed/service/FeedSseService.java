package com.kiero.feed.service;

import org.springframework.stereotype.Service;

import com.kiero.feed.domain.FeedItem;
import com.kiero.feed.presentation.dto.FeedPushPayload;
import com.kiero.global.infrastructure.sse.service.SseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedSseService {

	private final static String EVENT_NAME = "feed";
	private final SseService sseService;

	public void push(FeedItem feedItem) {
		sseService.push(
			key(feedItem.getParent().getId(), feedItem.getChild().getId()),
			EVENT_NAME,
			new FeedPushPayload(
				feedItem.getId(),
				feedItem.getEventType(),
				feedItem.getOccurredAt(),
				feedItem.getMetadata()
			)
		);
	}

	public String key(Long parentId, Long childId) {
		return "feed:" + parentId + ":" + childId;
	}

}
