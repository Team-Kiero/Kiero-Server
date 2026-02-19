package com.kiero.holidays.application.port.out;

import java.util.List;

import com.kiero.holidays.domain.Holiday;

public interface HolidayApiPort {
	List<Holiday> fetchHolidays(int year, int month);
}
