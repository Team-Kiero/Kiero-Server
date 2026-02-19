package com.kiero.holiday.application.port.out;

import java.util.List;

import com.kiero.holiday.domain.Holiday;

public interface HolidaySavePort {
	void saveAll(List<Holiday> holidays);
}
