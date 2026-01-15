package com.kiero.global.infrastructure.sse.repository;

import java.time.LocalDateTime;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SseEmitterWrapper {

	private final SseEmitter emitter;
	private final LocalDateTime expiresAt;
}
