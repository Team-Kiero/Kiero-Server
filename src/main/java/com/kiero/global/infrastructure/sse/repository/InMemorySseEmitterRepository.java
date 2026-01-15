package com.kiero.global.infrastructure.sse.repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class InMemorySseEmitterRepository implements SseEmitterRepository<String> {
	private final ConcurrentHashMap<String, SseEmitterWrapper> emitters = new ConcurrentHashMap<>();

	@Override
	public SseEmitterWrapper get(String key) {
		return emitters.get(key);
	}

	public void save(String key, SseEmitter emitter, LocalDateTime expiresAt) {
		emitters.put(key, new SseEmitterWrapper(emitter, expiresAt));
	}

	public Map<String, SseEmitterWrapper> findAll() {
		return emitters;
	}

	public void remove(String key) {
		emitters.remove(key);
	}

}
