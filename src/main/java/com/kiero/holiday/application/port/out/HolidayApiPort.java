package com.kiero.holiday.application.port.out;

import java.util.List;

import com.kiero.holiday.domain.Holiday;

public interface HolidayApiPort {
	List<Holiday> fetchHolidays(int year, int month);
}
