package com.kiero.global.sse.application.port.in;

import com.kiero.global.sse.domain.SseEventType;

public interface SsePushUseCase {
	void pushToParent(Long parentId, SseEventType eventType, Object data);
	void pushToChild(Long childId, SseEventType eventType, Object data);
}
