package com.kiero.global.sse.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.kiero.global.sse.application.port.in.SseBroadcastUseCase;
import com.kiero.global.sse.domain.SseEventType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseBroadcastService implements SseBroadcastUseCase {

	private final SseConnectionService sseConnectionService;

	@Override
	public void broadcastDateChangedSafely(LocalDate date) {
		try {
			sseConnectionService.broadcast(
				SseEventType.DATE_CHANGED.getEventName(),
				new DateChangedPayload(date)
			);
		} catch (Exception e) {
			log.error("DATE_CHANGED 브로드캐스트가 실패하였습니다. 실패 date={}", date, e);
		}
	}

	public record DateChangedPayload(LocalDate date) {}
}