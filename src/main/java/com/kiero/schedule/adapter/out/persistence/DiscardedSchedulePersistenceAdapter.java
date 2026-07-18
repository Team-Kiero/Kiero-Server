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
	public List<DiscardedSchedule> saveAll(List<DiscardedSchedule> discardedSchedules) {
		return discardedScheduleRepository.saveAll(discardedSchedules);
	}

	@Override
	public List<DiscardedSchedule> findAllByChildIdAndDayOfWeekIn(
		Long childId, List<DayOfWeek> dayOfWeeks
	) {
		return discardedScheduleRepository.findAllByChildIdAndDayOfWeekIn(childId, dayOfWeeks);
	}

	@Override
	public List<DiscardedSchedule> findAllByChildIdAndDateIn(
		Long childId, List<LocalDate> dates
	) {
		return discardedScheduleRepository.findAllByChildIdAndDateIn(childId, dates);
	}

	@Override
	public 	List<DiscardedSchedule> findAllByChildIdAndDateBetween(
		Long childId, LocalDate startDate, LocalDate endDate
	) {
		return discardedScheduleRepository.findAllByChildIdAndDateBetween(childId, startDate, endDate);
	}

	@Override
	public List<DiscardedSchedule> findAllByDate(LocalDate today) {
		return discardedScheduleRepository.findAllByDate(today);
	}

	@Override
	public void deleteByScheduleId(Long id) {
		discardedScheduleRepository.deleteByScheduleId(id);
	}
}
