package com.kiero.global.infrastructure.sse.repository;

import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterRepository<K> {
	String key(K key);
	void save(String key, SseEmitter emitter);
	SseEmitter get(String key);
	void remove (String key);
	Map<String, SseEmitter> findAll();
}
