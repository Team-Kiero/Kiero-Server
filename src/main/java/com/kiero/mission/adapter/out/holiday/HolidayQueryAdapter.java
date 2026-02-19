package com.kiero.mission.adapter.out.holiday;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.kiero.holidays.application.port.in.HolidayQueryUseCase;
import com.kiero.mission.application.port.out.HolidayQueryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HolidayQueryAdapter implements HolidayQueryPort {

	private final HolidayQueryUseCase holidayQueryUseCase;

	@Override
	public Set<LocalDate> getHolidayDatesBetween(LocalDate start, LocalDate end) {
		return holidayQueryUseCase.getHolidayDatesBetween(start, end);
	}
}