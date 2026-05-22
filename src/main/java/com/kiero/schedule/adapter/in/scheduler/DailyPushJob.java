package com.kiero.schedule.adapter.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyPushJob {

	private final Clock clock;
	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;

	@Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
	public void sendDailyStartNotifications() {
		LocalDate today = LocalDate.now(clock);
		scheduleSchedulerUseCase.sendDailyStartNotifications(today);
	}
}
