package com.kiero.schedule.adapter.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.global.sse.application.port.in.SseBroadcastUseCase;
import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyScheduleJob {

	private final Clock clock;

	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;
	private final SseBroadcastUseCase sseBroadcastUseCase;

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runDailyJob() {
		LocalDate today = LocalDate.now(clock);

		scheduleSchedulerUseCase.createTodayScheduleDetail();
		sseBroadcastUseCase.broadcastDateChangedSafely(today);
	}
}
