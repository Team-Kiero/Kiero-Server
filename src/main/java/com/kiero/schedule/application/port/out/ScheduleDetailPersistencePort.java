package com.kiero.schedule.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.kiero.schedule.domain.ScheduleDetail;

public interface ScheduleDetailPersistencePort {

	Optional<ScheduleDetail> findById(Long scheduleDetailId);

	List<ScheduleDetail> findByDateAndChildId(LocalDate date, Long childId);

	List<ScheduleDetail> findByDateInAndChildId(List<LocalDate> dates, Long childId);

	List<ScheduleDetail> findAllByScheduleIdInAndDateBetween(List<Long> scheduleIds, LocalDate startDate, LocalDate endDate);

	boolean existsStoneUsedToday(List<Long> scheduleIds, LocalDate date);

	List<ScheduleDetail> findAllByScheduleChildIdAndDateGreaterThanEqual(Long childId, LocalDate date);

	void saveAll(List<ScheduleDetail> details);

}
