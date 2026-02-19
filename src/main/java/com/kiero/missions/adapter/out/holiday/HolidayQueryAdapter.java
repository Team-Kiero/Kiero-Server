package com.kiero.missions.adapter.out.holiday;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.kiero.holiday.service.HolidayService;
import com.kiero.missions.application.port.out.HolidayQueryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HolidayQueryAdapter implements HolidayQueryPort {

	private final HolidayService holidayService;

	@Override
	public Set<LocalDate> getHolidayDatesBetween(LocalDate start, LocalDate end) {
		return holidayService.getHolidayDatesBetween(start, end);
	}
}