package com.kiero.global.infrastructure.sse.scheduler;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.infrastructure.sse.repository.InMemorySseEmitterRepository;
import com.kiero.global.infrastructure.sse.repository.SseEmitterWrapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {
	private final InMemorySseEmitterRepository emitterRepository;

	@Scheduled(fixedRate = 25_000)
	public void heartbeat() {
		Map<String, SseEmitterWrapper> emitters = emitterRepository.findAll();

		for (Map.Entry<String, SseEmitterWrapper> entry : emitters.entrySet()) {
			String key = entry.getKey();
			SseEmitterWrapper wrappedEmitter = entry.getValue();

			if (completeWhenTokenExpired(wrappedEmitter, key)) continue;

			SseEmitter emitter = wrappedEmitter.getEmitter();

			try {
				emitter.send(SseEmitter.event()
					.name("heartbeat")
					.data(Instant.now().toString()));
			} catch (IOException | IllegalStateException e) {
				emitterRepository.remove(key);
			}
		}
	}

	// accessToken 만료시각이 지나면 연결을 complete
	private boolean completeWhenTokenExpired(SseEmitterWrapper wrappedEmitter, String key) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime tokenExpiresAt = wrappedEmitter.getExpiresAt();
		SseEmitter emitter = wrappedEmitter.getEmitter();

		if (now.isAfter(tokenExpiresAt)) {
			try {
				emitter.complete();
			} catch (Exception ignore) { }

			emitterRepository.remove(key);
			return true;
		}
		return false;
	}
}
