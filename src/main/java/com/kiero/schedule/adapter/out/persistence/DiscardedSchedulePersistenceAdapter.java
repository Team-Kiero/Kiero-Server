package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.domain.DiscardedSchedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DiscardedSchedulePersistenceAdapter implements DiscardedSchedulePersistencePort {

	private final DiscardedScheduleRepository discardedScheduleRepository;

	@Override
	public DiscardedSchedule save(DiscardedSchedule discardedSchedule) {
		return discardedScheduleRepository.save(discardedSchedule);
	}

	@Override
	public List<DiscardedSchedule> findAllByChildIdAndDayOfWeekIn(
		Long childId, List<DayOfWeek> dayOfWeeks
	) {
		return discardedScheduleRepository.findAllByChildIdAndDateGreaterThanEqualAndDayOfWeekIn(childId, dayOfWeeks);
	}

	@Override
	public List<DiscardedSchedule> findAllByChildIdAndDateIn(
		Long childId, List<LocalDate> dates
	) {
		return discardedScheduleRepository.findAllByChildIdAndDateGreaterThanEqualAndDateIn(childId, dates);
	}

	@Override
	public 	List<DiscardedSchedule> findAllByChildIdAndDateBetween(
		Long childId, LocalDate startDate, LocalDate endDate
	) {
		return discardedScheduleRepository.findAllByChildIdAndDateBetween(childId, startDate, endDate);
	}
}
