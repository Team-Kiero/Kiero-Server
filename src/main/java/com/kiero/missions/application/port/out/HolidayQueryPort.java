package com.kiero.missions.application.port.out;

import java.time.LocalDate;
import java.util.Set;

public interface HolidayQueryPort {
	Set<LocalDate> getHolidayDatesBetween(LocalDate start, LocalDate end);
}
