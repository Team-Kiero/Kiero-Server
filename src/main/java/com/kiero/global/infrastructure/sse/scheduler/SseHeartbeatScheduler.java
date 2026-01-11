package com.kiero.global.infrastructure.sse.scheduler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.infrastructure.sse.repository.InMemorySseEmitterRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {
	private final InMemorySseEmitterRepository emitterRepository;

	@Scheduled(fixedRate = 25_000)
	public void heartbeat() {
		Map<String, SseEmitter> emitters = emitterRepository.findAll();

		for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
			String key = entry.getKey();
			SseEmitter emitter = entry.getValue();

			try {
				emitter.send(SseEmitter.event()
					.name("heartbeat")
					.data(Instant.now().toString()));
			} catch (IOException | IllegalStateException e) {
				emitterRepository.remove(key);
			}
		}
	}
}
