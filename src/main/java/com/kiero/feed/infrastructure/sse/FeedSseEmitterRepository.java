package com.kiero.feed.infrastructure.sse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class FeedSseEmitterRepository {

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public String key(Long parentId, Long childId) {
		return "P:" + parentId + ":C:" + childId;
	}

	public void save(String key, SseEmitter emitter) {
		emitters.put(key, emitter);
	}

	public SseEmitter get(String key) {
		return emitters.get(key);
	}

	public void remove(String key) {
		emitters.remove(key);
	}
}