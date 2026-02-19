package com.kiero.holidays.application.port.out;

import java.util.List;

import com.kiero.holidays.domain.Holiday;

public interface HolidaySavePort {
	void saveAll(List<Holiday> holidays);
}
