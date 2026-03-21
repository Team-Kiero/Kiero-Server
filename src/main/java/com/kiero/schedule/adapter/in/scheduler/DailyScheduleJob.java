package com.kiero.schedule.adapter.in.scheduler;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.global.sse.application.port.in.SseBroadcastUseCase;
import com.kiero.schedule.application.port.in.ScheduleSchedulerUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyScheduleJob {

	private static final String DAILY_JOB_KEY_PREFIX = "daily-job:executed:";
	private static final Duration DAILY_JOB_KEY_TTL = Duration.ofHours(48);

	private final Clock clock;
	private final StringRedisTemplate stringRedisTemplate;
	private final ScheduleSchedulerUseCase scheduleSchedulerUseCase;
	private final SseBroadcastUseCase sseBroadcastUseCase;

	@EventListener(ApplicationReadyEvent.class)
	public void recoverIfMissed() {
		LocalDate today = LocalDate.now(clock);
		String key = DAILY_JOB_KEY_PREFIX + today;

		if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
			log.warn("오늘({}) DailySchedule job 미실행 감지 - 보정 실행", today);
			runDailyJob();
		}
	}
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
	public void runDailyJob() {
		LocalDate today = LocalDate.now(clock);

		scheduleSchedulerUseCase.createTodayScheduleDetail();
		scheduleSchedulerUseCase.deleteObsoleteNonRecurringSchedules();
		sseBroadcastUseCase.broadcastDateChangedSafely(today);

		String key = DAILY_JOB_KEY_PREFIX + today;
		stringRedisTemplate.opsForValue().set(key, "1", DAILY_JOB_KEY_TTL);
		log.info("오늘({}) DailySchedule job 완료 - Redis 플래그 저장", today);
	}
}
