package com.kiero.feed.infrastructure.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.feed.exception.FeedErrorCode;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedSseService {

	private static final long TIMEOUT = 60L * 60 * 1000;

	private final FeedSseEmitterRepository emitterRepository;

	public SseEmitter subscribe(Long parentId, Long childId) {
		String key = emitterRepository.key(parentId, childId);

		SseEmitter emitter = new SseEmitter(TIMEOUT);
		emitterRepository.save(key, emitter);

		emitter.onCompletion(() -> emitterRepository.remove(key));
		emitter.onTimeout(() -> emitterRepository.remove(key));
		emitter.onError(e -> emitterRepository.remove(key));

		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("subscribed"));
		} catch (Exception e) {
			emitterRepository.remove(key);
		}

		return emitter;
	}

	public void push(Long parentId, Long childId, Object data) {
		sendIfExists(emitterRepository.key(parentId, childId), data);
	}

	public void sendIfExists(String key, Object data) {
		SseEmitter emitter = emitterRepository.get(key);
		if (emitter == null) throw new KieroException(FeedErrorCode.FEED_SSE_SUBSCRIBE_FAILED);
		try {
			emitter.send(SseEmitter.event()
				.name("feed")
				.data(data));
		} catch (Exception e) {
			emitterRepository.remove(key);
		}
	}
}
