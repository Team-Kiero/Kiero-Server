package com.kiero.global.sse.application.port.out;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.sse.domain.SseEmitterWrapper;

public interface SseEmitterRepositoryPort {
	SseEmitterWrapper get(String key);

	void save(String key, SseEmitter emitter, LocalDateTime expiresAt);

	Map<String, SseEmitterWrapper> findAll();

	void remove(String key);
}
