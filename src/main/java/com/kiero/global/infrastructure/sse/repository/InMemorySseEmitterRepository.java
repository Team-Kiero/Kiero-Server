package com.kiero.global.infrastructure.sse.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class InMemorySseEmitterRepository implements SseEmitterRepository<String> {
	private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	@Override public String key(String key) { return key; }
	@Override public void save(String key, SseEmitter emitter) { emitters.put(key, emitter); }
	@Override public SseEmitter get(String key) { return emitters.get(key); }
	@Override public void remove(String key) { emitters.remove(key); }
	@Override public Map<String, SseEmitter> findAll() { return Map.copyOf(emitters); }
}
