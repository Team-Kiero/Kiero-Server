package com.kiero.global.sse.application.port.in;

import java.time.LocalDate;

public interface SseBroadcastUseCase {
	void broadcastDateChangedSafely(LocalDate date);
}
