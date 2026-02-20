package com.kiero.global.sse.application.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.sse.application.port.in.SseSubscribeUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseSubscribeService implements SseSubscribeUseCase {

	private static final String PARENT_KEY_PREFIX = "events:parent:";
	private static final String CHILD_KEY_PREFIX = "events:child:";

	private final SseConnectionService sseConnectionService;

	@Override
	public SseEmitter subscribeAsParent(Long parentId, String token) {
		String key = PARENT_KEY_PREFIX + parentId;
		log.info("부모 SSE 구독: parentId={}", parentId);
		return sseConnectionService.subscribe(key, token);
	}

	@Override
	public SseEmitter subscribeAsChild(Long childId, String token) {
		String key = CHILD_KEY_PREFIX + childId;
		log.info("자녀 SSE 구독: childId={}", childId);
		return sseConnectionService.subscribe(key, token);
	}
}
