package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.schedule.application.dto.ScheduleOccurrenceDto;
import com.kiero.schedule.application.dto.ScheduleOccurrencesResponse;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.vo.DiscardKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleCacheableService {

	private final SchedulePersistencePort schedulePersistencePort;
	private final ScheduleRepeatDaysPersistencePort scheduleRepeatDaysPersistencePort;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;
	private final Clock clock;

	@Transactional
	@Cacheable(cacheNames = "schedules", key = "#childId + ':' + #startDate + ':' + #endDate")
	public ScheduleOccurrencesResponse getSchedules(LocalDate startDate, LocalDate endDate, Long childId) {
		List<Schedule> schedules = schedulePersistencePort.findAllByChildId(childId);
		if (schedules.isEmpty()) {
			return ScheduleOccurrencesResponse.of(false, List.of());
		}

		boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId, LocalDate.now(clock));

		LocalDate today = LocalDate.now(clock);

		Set<DiscardKey> discardedKeys = discardedSchedulePersistencePort
			.findAllByChildIdAndDateBetween(childId, startDate, endDate).stream()
			.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
			.collect(Collectors.toSet());

		List<Long> recurringIds = schedules.stream()
			.filter(Schedule::isRecurring)
			.map(Schedule::getId)
			.toList();

		Map<Long, List<DayOfWeek>> repeatDaysByScheduleId = Map.of();
		if (!recurringIds.isEmpty()) {
			List<ScheduleRepeatDays> repeatDays = scheduleRepeatDaysPersistencePort.findAllByScheduleIdsIn(recurringIds);
			repeatDaysByScheduleId = repeatDays.stream()
				.collect(Collectors.groupingBy(
					rd -> rd.getSchedule().getId(),
					Collectors.mapping(ScheduleRepeatDays::getDayOfWeek, Collectors.toList())
				));
		}

		List<Long> normalIds = schedules.stream()
			.filter(s -> !s.isRecurring())
			.map(Schedule::getId)
			.toList();

		Map<Long, Schedule> scheduleById = schedules.stream().collect(Collectors.toMap(Schedule::getId, s -> s));

		List<ScheduleOccurrenceDto> items = new java.util.ArrayList<>();

		Map<Long, ScheduleDetail> todayScheduleDetailByScheduleId = Map.of();
		if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
			todayScheduleDetailByScheduleId = scheduleDetailPersistencePort.findByDateAndChildId(today, childId).stream()
				.collect(Collectors.toMap(
					sd -> sd.getSchedule().getId(),
					sd -> sd
				));
		}

		if (!normalIds.isEmpty()) {
			List<ScheduleDetail> details = scheduleDetailPersistencePort.findAllByScheduleIdInAndDateBetween(normalIds, startDate, endDate);

			for (ScheduleDetail d : details) {
				Schedule s = scheduleById.get(d.getSchedule().getId());
				ScheduleStatus todayScheduleStatus = d.getDate().isEqual(today) ? d.getScheduleStatus() : null;

				if (discardedKeys.contains(new DiscardKey(s.getId(), d.getDate())))
					continue;

				items.add(new ScheduleOccurrenceDto(
					s.getId(),
					d.getDate(),
					List.of(),
					todayScheduleStatus,
					s.getStartTime(),
					s.getEndTime(),
					s.getName(),
					s.getScheduleColor().getColorCode()
				));
			}
		}

		for (Schedule s : schedules) {
			if (!s.isRecurring())
				continue;

			List<DayOfWeek> repeatDays = repeatDaysByScheduleId.getOrDefault(s.getId(), List.of());
			if (repeatDays.isEmpty())
				continue;

			LocalDate repeatStart = s.getRepeatStartDate();
			LocalDate repeatEnd = s.getRepeatEndDate();

			for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {
				if (repeatStart != null && cursor.isBefore(repeatStart))
					continue;
				if (repeatEnd != null && cursor.isAfter(repeatEnd))
					continue;

				DayOfWeek cursorDow = DayOfWeek.from(cursor.getDayOfWeek());
				if (!repeatDays.contains(cursorDow))
					continue;

				if (discardedKeys.contains(new DiscardKey(s.getId(), cursor)))
					continue;

				ScheduleStatus todayScheduleStatus = null;
				if (cursor.isEqual(today)) {
					ScheduleDetail todayDetail = todayScheduleDetailByScheduleId.get(s.getId());
					todayScheduleStatus = todayDetail != null ? todayDetail.getScheduleStatus() : null;
				}

				items.add(new ScheduleOccurrenceDto(
					s.getId(),
					cursor,
					repeatDays,
					todayScheduleStatus,
					s.getStartTime(),
					s.getEndTime(),
					s.getName(),
					s.getScheduleColor().getColorCode()
				));
			}
		}

		items.sort(
			Comparator.comparing(ScheduleOccurrenceDto::date)
				.thenComparing(ScheduleOccurrenceDto::startTime)
				.thenComparing(ScheduleOccurrenceDto::scheduleId)
		);

		return ScheduleOccurrencesResponse.of(isFireLitToday, items);
	}
}
