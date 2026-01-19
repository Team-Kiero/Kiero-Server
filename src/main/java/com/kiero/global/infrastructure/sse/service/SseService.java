package com.kiero.global.infrastructure.sse.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.auth.jwt.service.JwtTokenProvider;
import com.kiero.global.infrastructure.sse.repository.SseEmitterRepository;
import com.kiero.global.infrastructure.sse.repository.SseEmitterWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {
	private final SseEmitterRepository<String> emitterRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public SseEmitter subscribe(String key, String token) {
		LocalDateTime tokenExpiresAt = jwtTokenProvider.getExpirationDateTime(token);

		// 토큰 만료 시간까지 SSE 연결 유지
		long timeoutMillis = Duration.between(LocalDateTime.now(), tokenExpiresAt).toMillis();
		SseEmitter emitter = new SseEmitter(timeoutMillis);

		emitterRepository.save(key, emitter, tokenExpiresAt);

		log.info("wrapped emitter expires at : {}", tokenExpiresAt);

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