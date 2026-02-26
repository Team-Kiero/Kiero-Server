package com.kiero.schedule.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.kiero.schedule.domain.DiscardedSchedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

public interface DiscardedSchedulePersistencePort {
	DiscardedSchedule save(DiscardedSchedule discardedSchedule);

	List<DiscardedSchedule> findAllByChildIdAndDayOfWeekIn(
		Long childId, List<DayOfWeek> dayOfWeeks
	);

	List<DiscardedSchedule> findAllByChildIdAndDateIn(
		Long childId, List<LocalDate> dates
	);
}
