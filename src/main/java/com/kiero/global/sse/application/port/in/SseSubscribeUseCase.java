package com.kiero.global.sse.application.port.in;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseSubscribeUseCase {
	SseEmitter subscribeAsParent(Long parentId, String token);
	SseEmitter subscribeAsChild(Long childId, String token);
}
