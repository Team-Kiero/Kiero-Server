package com.kiero.schedule.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.kiero.schedule.application.dto.ScheduleUpdateEventTarget;
import com.kiero.schedule.domain.ScheduleDetail;

public interface ScheduleDetailPersistencePort {

	Optional<ScheduleDetail> findById(Long scheduleDetailId);

	List<ScheduleDetail> findByDateAndChildId(LocalDate date, Long childId);

	List<ScheduleDetail> findAllByDate(LocalDate date);

	List<ScheduleDetail> findByDateInAndChildId(List<LocalDate> dates, Long childId);

	List<ScheduleDetail> findAllByScheduleIdInAndDateBetween(List<Long> scheduleIds, LocalDate startDate,
		LocalDate endDate);

	boolean existsStoneUsedTodayByChildIdAndDate(Long childId, LocalDate date);

	List<ScheduleDetail> findAllByScheduleChildIdAndDateGreaterThanEqual(Long childId, LocalDate date);

	ScheduleDetail save(ScheduleDetail scheduleDetail);

	List<ScheduleDetail> saveAll(List<ScheduleDetail> details);

	void deleteByScheduleIdAndDate(Long scheduleId, LocalDate date);

	Optional<ScheduleDetail> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

	boolean existsByScheduleIdAndDate(Long scheduleId, LocalDate date);

	void deleteScheduleDetail(ScheduleDetail scheduleDetail);

	List<ScheduleUpdateEventTarget> findScheduleUpdateEventTarget(LocalDate today, LocalTime now);

	void bulkMarkPendingAsFailed(LocalDate today, LocalTime now);

	void bulkMarkVerifiedAsCompleted(LocalDate today, LocalTime now);

	boolean existsByDateAndChildIdAfterEndTime(LocalDate date, Long childId, LocalTime endTime);

	Optional<ScheduleDetail> findByIdWithSchedule(Long scheduleDetailId);

	void deleteAllByScheduleId(Long scheduleId);
}
