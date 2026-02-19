package com.kiero.holidays.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.kiero.holidays.application.port.in.FetchHolidayUseCase;
import com.kiero.holidays.application.port.out.HolidayApiPort;
import com.kiero.holidays.application.port.out.HolidaySavePort;
import com.kiero.holidays.domain.Holiday;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayFetchService implements FetchHolidayUseCase {

	private final HolidayApiPort holidayApiPort;
	private final HolidaySavePort holidaySavePort;

	@Override
	public void fetchAndSaveHolidays() {
		int currentYear = LocalDate.now().getYear();
		fetchByYear(currentYear);
		fetchByYear(currentYear + 1);
	}

	private void fetchByYear(int year) {
		for (int month = 1; month <= 12; month++) {
			List<Holiday> holidays =
				holidayApiPort.fetchHolidays(year, month);

			if (!holidays.isEmpty()) {
				holidaySavePort.saveAll(holidays);
				log.info("Saved {} holidays for {}-{}", holidays.size(), year, month);
			}
		}
	}
}