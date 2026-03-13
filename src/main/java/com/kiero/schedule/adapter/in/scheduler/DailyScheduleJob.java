package com.kiero.schedule.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyScheduleJob {

	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runDailyJob() {
		scheduleSchedulerUseCase.createTodayScheduleDetail();
		scheduleSchedulerUseCase.publishDateChangedEvent();
	}
}
