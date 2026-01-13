package com.kiero.holiday.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.holiday.service.HolidayService;

@Component
@RequiredArgsConstructor
public class DailyHolidayJob {

    private final HolidayService holidayService;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailyJob() {
            holidayService.fetchAndSaveHolidays();
    }
}