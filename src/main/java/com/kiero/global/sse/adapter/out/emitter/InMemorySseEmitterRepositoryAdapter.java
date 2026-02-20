package com.kiero.global.sse.adapter.out.emitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.sse.application.port.out.SseEmitterRepositoryPort;
import com.kiero.global.sse.domain.SseEmitterWrapper;

@Component
public class InMemorySseEmitterRepositoryAdapter implements SseEmitterRepositoryPort {

	private final ConcurrentHashMap<String, SseEmitterWrapper> emitters = new ConcurrentHashMap<>();

	@Override
	public SseEmitterWrapper get(String key) {
		return emitters.get(key);
	}

	@Override
	public void save(String key, SseEmitter emitter, LocalDateTime expiresAt) {
		emitters.put(key, new SseEmitterWrapper(emitter, expiresAt));
	}

	@Override
	public Map<String, SseEmitterWrapper> findAll() {
		return emitters;
	}

	@Override
	public void remove(String key) {
		emitters.remove(key);
	}
}
