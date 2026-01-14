package com.kiero.holiday.service;

import com.kiero.holiday.domain.Holiday;
import com.kiero.holiday.dto.HolidayApiResponse;
import com.kiero.holiday.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Value("${open-api.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void fetchAndSaveHolidays() {
        int currentYear = LocalDate.now().getYear();
        fetchAndSaveHolidaysByYear(currentYear);
        fetchAndSaveHolidaysByYear(currentYear + 1);
    }

    public void fetchAndSaveHolidaysByYear(int year) {
        for (int month = 1; month <= 12; month++) {
            fetchAndSaveHolidaysByMonth(year, month);
        }
    }

    private void fetchAndSaveHolidaysByMonth(int year, int month) {
        try {
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(
                            "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"
                    )
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("solYear", year)
                    .queryParam("solMonth", String.format("%02d", month))
                    .build(true)
                    .toUri();

            log.debug("Holiday API URI = {}", uri);

            HolidayApiResponse response =
                    restTemplate.getForObject(uri, HolidayApiResponse.class);

            if (!isValidResponse(response)) {
                log.info("No holiday data for {}-{}", year, month);
                return;
            }

            List<Holiday> holidays = response.body().items().itemList().stream()
                    .filter(i -> "Y".equals(i.isHoliday()))
                    .map(this::toEntity)
                    .toList();

            if (!holidays.isEmpty()) {
                holidayRepository.saveAll(holidays);
                log.info("Saved {} holidays for {}-{}", holidays.size(), year, month);
            }

            // API 과부하 방지
            Thread.sleep(300);

        } catch (Exception e) {
            log.error("Failed to fetch holidays for {}-{}", year, month, e);
        }
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsById(date);
    }

    private boolean isValidResponse(HolidayApiResponse res) {
        return res != null
                && res.body() != null
                && res.body().items() != null
                && res.body().items().itemList() != null
                && !res.body().items().itemList().isEmpty();
    }

    private Holiday toEntity(HolidayApiResponse.Item item) {
        return Holiday.builder()
                .date(parseDate(item.locdate()))
                .name(item.dateName())
                .build();
    }

    private LocalDate parseDate(Integer locdate) {
        return LocalDate.parse(
                String.valueOf(locdate),
                DateTimeFormatter.BASIC_ISO_DATE
        );
    }
}