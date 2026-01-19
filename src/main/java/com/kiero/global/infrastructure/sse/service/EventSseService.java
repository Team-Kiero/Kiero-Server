package com.kiero.global.infrastructure.sse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.infrastructure.sse.dto.SsePayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSseService {

	private static final String EVENT_NAME = "event";
	private static final String PARENT_KEY_PREFIX = "events:parent:";
	private static final String CHILD_KEY_PREFIX = "events:child:";

	private final SseService sseService;

	// 부모의 통합 SSE 구독
	public SseEmitter subscribeAsParent(Long parentId, String token) {
		String key = createParentKey(parentId);
		log.info("부모 SSE 구독: parentId={}", parentId);
		return sseService.subscribe(key, token);
	}

	// 자녀의 통합 SSE 구독
	public SseEmitter subscribeAsChild(Long childId, String token) {
		String key = createChildKey(childId);
		log.info("자녀 SSE 구독: childId={}", childId);
		return sseService.subscribe(key, token);
	}

	// 부모에게 이벤트 푸시
	public void pushToParent(Long parentId, SsePayload payload) {
		String key = createParentKey(parentId);
		log.debug("부모 SSE 푸시: parentId={}, eventType={}", parentId, payload.eventType());
		sseService.push(key, EVENT_NAME, payload);
	}

	// 자녀에게 이벤트 푸시
	public void pushToChild(Long childId, SsePayload payload) {
		String key = createChildKey(childId);
		log.debug("자녀 SSE 푸시: childId={}, eventType={}", childId, payload.eventType());
		sseService.push(key, EVENT_NAME, payload);
	}

	private String createParentKey(Long parentId) {
		return PARENT_KEY_PREFIX + parentId;
	}

	private String createChildKey(Long childId) {
		return CHILD_KEY_PREFIX + childId;
	}
}
