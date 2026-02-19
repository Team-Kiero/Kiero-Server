package com.kiero.holiday.application.port.out;

import java.time.LocalDate;
import java.util.Set;

public interface HolidayLoadPort {
	Set<LocalDate> findHolidayDatesBetween(LocalDate startDate, LocalDate endDate);
}
