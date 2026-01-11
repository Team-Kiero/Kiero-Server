package com.kiero.global.infrastructure.sse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.infrastructure.sse.repository.SseEmitterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SseService {
	private static final long TIMEOUT = 60L * 60 * 1000;
	private final SseEmitterRepository<String> emitterRepository;

	public SseEmitter subscribe(String key) {
		SseEmitter emitter = new SseEmitter(TIMEOUT);
		emitterRepository.save(key, emitter);

		emitter.onCompletion(() -> emitterRepository.remove(key));
		emitter.onTimeout(() -> emitterRepository.remove(key));
		emitter.onError(e -> emitterRepository.remove(key));

		safeSend(key, "connected", "subscribed");
		return emitter;
	}

	public void push(String key, String eventName, Object data) {
		safeSend(key, eventName, data);
	}

	private void safeSend(String key, String eventName, Object data) {
		SseEmitter emitter = emitterRepository.get(key);
		if (emitter == null) return;

		try {
			emitter.send(SseEmitter.event().name(eventName).data(data));
		} catch (Exception e) {
			emitterRepository.remove(key);
		}
	}
}