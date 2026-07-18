package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.schedule.application.dto.ScheduleEventTarget;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.domain.ScheduleDetail;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduleDetailPersistenceAdapter implements ScheduleDetailPersistencePort {

	private final ScheduleDetailRepository scheduleDetailRepository;

	@Override
	public Optional<ScheduleDetail> findById(Long scheduleDetailId) {
		return scheduleDetailRepository.findById(scheduleDetailId);
	}

	@Override
	public List<ScheduleDetail> findByDateAndChildId(LocalDate date, Long childId) {
		return scheduleDetailRepository.findByDateAndChildId(date, childId);
	}

	@Override
	public List<ScheduleDetail> findAllByDate(LocalDate date) {
		return scheduleDetailRepository.findAllByDate(date);
	}

	@Override
	public List<ScheduleDetail> findByDateInAndChildId(List<LocalDate> dates, Long childId) {
		return scheduleDetailRepository.findByDateInAndChildId(dates, childId);
	}

	@Override
	public List<ScheduleDetail> findAllByScheduleIdInAndDateBetween(List<Long> scheduleIds, LocalDate startDate,
		LocalDate endDate) {
		return scheduleDetailRepository.findAllByScheduleIdInAndDateBetween(scheduleIds, startDate, endDate);
	}

	@Override
	public boolean existsStoneUsedTodayByChildIdAndDate(Long childId, LocalDate date) {
		return scheduleDetailRepository.existsStoneUsedTodayByChildIdAndDate(childId, date);
	}

	@Override
	public List<ScheduleDetail> findAllByScheduleChildIdAndDateGreaterThanEqual(Long childId, LocalDate date) {
		return scheduleDetailRepository.findAllByScheduleChildIdAndDateGreaterThanEqual(childId, date);
	}

	@Override
	public ScheduleDetail save(ScheduleDetail scheduleDetail) {
		return scheduleDetailRepository.save(scheduleDetail);
	}

	@Override
	public List<ScheduleDetail> saveAll(List<ScheduleDetail> details) {
		return scheduleDetailRepository.saveAll(details);
	}

	@Override
	public void deleteByScheduleIdAndDate(Long scheduleId, LocalDate date) {
		scheduleDetailRepository.deleteByScheduleIdAndDate(scheduleId, date);
	}

	@Override
	public Optional<ScheduleDetail> findByScheduleIdAndDate(Long scheduleId, LocalDate date) {
		return scheduleDetailRepository.findByScheduleIdAndDate(scheduleId, date);
	}

	@Override
	public boolean existsByScheduleIdAndDate(Long scheduleId, LocalDate date) {
		return scheduleDetailRepository.existsByScheduleIdAndDate(scheduleId, date);
	}

	@Override
	public void deleteScheduleDetail(ScheduleDetail scheduleDetail) {
		scheduleDetailRepository.delete(scheduleDetail);
	}

	@Override
	public List<ScheduleEventTarget> findScheduleUpdateEventTarget(LocalDate today, LocalTime now) {
		return scheduleDetailRepository.findScheduleUpdateEventTargets(today, now);
	}

	@Override
	public List<ScheduleEventTarget> findScheduleStartEventTarget(LocalDate today, LocalTime now) {
		LocalTime nowStart = now.withSecond(0).withNano(0);
		LocalTime nowEnd = nowStart.plusMinutes(1);
		return scheduleDetailRepository.findScheduleStartEventTargets(today, nowStart, nowEnd);
	}

	@Override
	public void bulkMarkPendingAsFailed(LocalDate today, LocalTime now) {
		scheduleDetailRepository.bulkMarkPendingAsFailed(today, now);
	}

	@Override
	public void bulkMarkVerifiedAsCompleted(LocalDate today, LocalTime now) {
		scheduleDetailRepository.bulkMarkVerifiedAsCompleted(today, now);
	}

	@Override
	public boolean existsByDateAndChildIdAfterEndTime(LocalDate today, Long childId, LocalTime endTime) {
		return scheduleDetailRepository.existsByDateAndChildIdAfterEndTime(today, childId, endTime);
	}

	@Override
	public Optional<ScheduleDetail> findByIdWithSchedule(Long scheduleDetailId) {
		return scheduleDetailRepository.findByIdWithSchedule(scheduleDetailId);
	}

	@Override
	public void deleteAllByScheduleId(Long scheduleId) {
		scheduleDetailRepository.deleteAllByScheduleId(scheduleId);
	}

	@Override
	public List<Long> findDistinctChildIdsByDate(LocalDate today) {
		return scheduleDetailRepository.findDistinctChildIdsByDate(today);
	}

	@Override
	public List<ScheduleDetail> findFailedPastEndTimeWithoutReminder(LocalDate today, LocalTime now) {
		return scheduleDetailRepository.findFailedPastEndTimeWithoutReminder(today, now);
	}

	@Override
	public List<ScheduleDetail> findPendingByStartTimeWindow(LocalDate today, LocalTime targetStart, LocalTime targetEnd) {
		return scheduleDetailRepository.findPendingByStartTimeWindow(today, targetStart, targetEnd);
	}

}
