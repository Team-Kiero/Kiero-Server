package com.kiero.holiday.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kiero.holiday.application.port.in.FetchHolidayUseCase;

@Component
@RequiredArgsConstructor
public class DailyHolidayJob {

    private final FetchHolidayUseCase fetchHolidayUseCase;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailyJob() {
        fetchHolidayUseCase.fetchAndSaveHolidays();
    }
}