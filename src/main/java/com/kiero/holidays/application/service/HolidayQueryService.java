package com.kiero.holidays.application.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.kiero.holidays.application.port.in.HolidayQueryUseCase;
import com.kiero.holidays.application.port.out.HolidayLoadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HolidayQueryService implements HolidayQueryUseCase {

	private final HolidayLoadPort holidayLoadPort;

	@Override
	public Set<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate) {
		return holidayLoadPort.findHolidayDatesBetween(startDate, endDate);
	}
}