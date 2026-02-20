package com.kiero.global.sse.application.service;

import org.springframework.stereotype.Service;

import com.kiero.global.sse.application.port.in.SsePushUseCase;
import com.kiero.global.sse.domain.SseEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SsePushService implements SsePushUseCase {

	private static final String PARENT_KEY_PREFIX = "events:parent:";
	private static final String CHILD_KEY_PREFIX = "events:child:";

	private final SseConnectionService sseConnectionService;

	@Override
	public void pushToParent(Long parentId, SseEventType eventType, Object data) {
		String key = PARENT_KEY_PREFIX + parentId;
		log.debug("부모 SSE 푸시: parentId={}, eventType={}", parentId, eventType);
		sseConnectionService.push(key, eventType.getEventName(), data);
	}

	@Override
	public void pushToChild(Long childId, SseEventType eventType, Object data) {
		String key = CHILD_KEY_PREFIX + childId;
		log.debug("자녀 SSE 푸시: childId={}, eventType={}", childId, eventType);
		sseConnectionService.push(key, eventType.getEventName(), data);
	}
}