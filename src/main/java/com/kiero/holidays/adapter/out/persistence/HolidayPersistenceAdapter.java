package com.kiero.holidays.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kiero.holidays.application.port.out.HolidayLoadPort;
import com.kiero.holidays.application.port.out.HolidaySavePort;
import com.kiero.holidays.domain.Holiday;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HolidayPersistenceAdapter implements HolidaySavePort, HolidayLoadPort {

	private final HolidayRepository holidayRepository;

	@Override
	public void saveAll(List<Holiday> holidays) {
		holidayRepository.saveAll(holidays);
	}

	@Override
	public Set<LocalDate> findHolidayDatesBetween(LocalDate startDate, LocalDate endDate) {
		return holidayRepository.findByDateBetween(startDate, endDate)
			.stream()
			.map(Holiday::getDate)
			.collect(Collectors.toSet());
	}
}
