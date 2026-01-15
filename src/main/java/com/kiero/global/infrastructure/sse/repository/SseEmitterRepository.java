package com.kiero.global.infrastructure.sse.repository;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterRepository<K> {
	SseEmitterWrapper get(String key);

	void save(String key, SseEmitter emitter, LocalDateTime expiresAt);

	Map<String, SseEmitterWrapper> findAll();

	void remove(String key);
}
