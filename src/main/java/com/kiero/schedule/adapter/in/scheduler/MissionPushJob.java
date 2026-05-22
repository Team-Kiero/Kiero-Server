package com.kiero.schedule.adapter.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionPushJob {

	private final Clock clock;
	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;

	@Scheduled(cron = "0 0 17 * * *", zone = "Asia/Seoul")
	public void sendMissionIncompleteNotifications() {
		LocalDate today = LocalDate.now(clock);
		scheduleSchedulerUseCase.sendMissionIncompleteNotifications(today);
	}
}
