package com.kiero.global.sse.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.sse.application.port.out.SseEmitterRepositoryPort;
import com.kiero.global.sse.application.port.out.TokenExpiryPort;
import com.kiero.global.sse.domain.SseEmitterWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseConnectionService {

	private static final long DEFAULT_TIMEOUT_MILLIS = 30 * 60 * 1000L;

	private final SseEmitterRepositoryPort emitterRepository;
	private final TokenExpiryPort tokenExpiryPort;

	public SseEmitter subscribe(String key, String token) {
		LocalDateTime tokenExpiresAt = tokenExpiryPort.getExpirationDateTime(token);

		SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);

		emitterRepository.save(key, emitter, tokenExpiresAt);

		log.info("SSE 구독 생성: key={}, tokenExpiresAt={}", key, tokenExpiresAt);

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
		SseEmitterWrapper emitter = emitterRepository.get(key);
		if (emitter == null) return;

		try {
			emitter.getEmitter().send(SseEmitter.event().name(eventName).data(data));
		} catch (Exception e) {
			emitterRepository.remove(key);
		}
	}
}