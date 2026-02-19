package com.kiero.holiday.service;

import com.kiero.holidays.application.port.in.FetchHolidayUseCase;
import com.kiero.holidays.domain.Holiday;
import com.kiero.holidays.application.dto.HolidayApiResponse;
import com.kiero.holidays.adapter.out.persistence.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DailyHolidayJob {

    private final FetchHolidayUseCase fetchHolidayUseCase;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailyJob() {
        fetchHolidayUseCase.fetchAndSaveHolidays();
    }
}