package com.kiero.holiday.application.port.in;

import java.time.LocalDate;
import java.util.Set;

public interface HolidayQueryUseCase {
	Set<LocalDate> getHolidayDatesBetween(LocalDate startDate, LocalDate endDate);
}
