package com.kiero.schedule.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.schedule.application.service.ScheduleCommandService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DailyScheduleJob {

	private final ScheduleCommandService scheduleCommandService;

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runDailyJob() {
		scheduleCommandService.createTodayScheduleDetail();

	}
}
