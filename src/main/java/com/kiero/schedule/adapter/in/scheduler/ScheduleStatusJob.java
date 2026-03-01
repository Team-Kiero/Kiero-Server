package com.kiero.schedule.adapter.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleStatusJob {

	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;
	private final Clock clock;

	@Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
	@Transactional
	public void updateScheduleStatuses() {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		scheduleSchedulerUseCase.bulkMarkAndPushEventIfUpdateExists(today, now);

	}
}