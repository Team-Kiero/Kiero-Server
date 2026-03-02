package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.application.dto.ScheduleOccurrenceDto;
import com.kiero.schedule.application.dto.ScheduleOccurrencesResponse;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleColor;
import com.kiero.schedule.domain.vo.DiscardKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService implements ScheduleQueryUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	private final SchedulePersistencePort schedulePort;
	private final ScheduleRepeatDaysPersistencePort repeatDaysPort;
	private final ScheduleDetailPersistencePort detailPort;

	private final Clock clock;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;

	@Override
	@Transactional
	public DefaultScheduleContentResponse getDefaultSchedule(Long parentId, Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		ScheduleColor nextColor = schedulePort.findFirstByChildIdOrderByCreatedAtDesc(childId)
			.map(Schedule::getScheduleColor)
			.map(ScheduleColor::next)
			.orElse(ScheduleColor.SCHEDULE1);

		return new DefaultScheduleContentResponse(nextColor, nextColor.getColorCode());
	}

	@Override
	@Transactional
	public ScheduleOccurrencesResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId,
		Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		if (startDate.isAfter(endDate) || endDate.isBefore(startDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		List<Schedule> schedules = schedulePort.findAllByChildId(childId);
		if (schedules.isEmpty()) {
			return ScheduleOccurrencesResponse.of(false, List.of());
		}

		boolean isFireLitToday = detailPort.existsStoneUsedTodayByChildIdAndDate(childId, LocalDate.now(clock));

		// 반복일정 일회성 수정, 일회성 삭제 등으로 인해 무시되어야 하는 일정 집합
		Set<DiscardKey> discardedKeys = discardedSchedulePersistencePort
			.findAllByChildIdAndDateBetween(childId, startDate, endDate).stream()
			.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
			.collect(Collectors.toSet());

		// 반복일정 가져오기
		List<Long> recurringIds = schedules.stream()
			.filter(Schedule::isRecurring)
			.map(Schedule::getId)
			.toList();

		Map<Long, List<DayOfWeek>> repeatDaysByScheduleId = Map.of();
		if (!recurringIds.isEmpty()) {
			List<ScheduleRepeatDays> repeatDays = repeatDaysPort.findAllByScheduleIdsIn(recurringIds);

			repeatDaysByScheduleId = repeatDays.stream()
				.collect(Collectors.groupingBy(
					rd -> rd.getSchedule().getId(),
					Collectors.mapping(ScheduleRepeatDays::getDayOfWeek, Collectors.toList())
				));
		}

		// 단일일정 가져오기
		List<Long> normalIds = schedules.stream()
			.filter(s -> !s.isRecurring())
			.map(Schedule::getId)
			.toList();

		Map<Long, Schedule> scheduleById = schedules.stream().collect(Collectors.toMap(Schedule::getId, s -> s));

		List<ScheduleOccurrenceDto> items = new java.util.ArrayList<>();

		// 단일일정들 dto화
		if (!normalIds.isEmpty()) {
			List<ScheduleDetail> details = detailPort.findAllByScheduleIdInAndDateBetween(normalIds, startDate,
				endDate);

			for (ScheduleDetail d : details) {
				Schedule s = scheduleById.get(d.getSchedule().getId());

				// discarded 된 건 제외
				if (discardedKeys.contains(new DiscardKey(s.getId(), d.getDate())))
					continue;

				items.add(new ScheduleOccurrenceDto(
					s.getId(),
					d.getDate(),
					s.getStartTime(),
					s.getEndTime(),
					s.getName(),
					s.getScheduleColor().getColorCode()
				));
			}
		}

		// 반복 일정 dto화
		for (Schedule s : schedules) {
			if (!s.isRecurring())
				continue;

			List<DayOfWeek> repeatDays = repeatDaysByScheduleId.getOrDefault(s.getId(), List.of());
			if (repeatDays.isEmpty())
				continue;

			LocalDate repeatStart = s.getRepeatStartDate();
			LocalDate repeatEnd = s.getRepeatEndDate();

			for (LocalDate cursor = startDate; !cursor.isAfter(endDate); cursor = cursor.plusDays(1)) {

				// 기간 조건
				if (repeatStart != null && cursor.isBefore(repeatStart))
					continue;
				if (repeatEnd != null && cursor.isAfter(repeatEnd))
					continue;

				// 요일 조건
				DayOfWeek cursorDow = DayOfWeek.from(cursor.getDayOfWeek());
				if (!repeatDays.contains(cursorDow))
					continue;

				// discarded 제외
				if (discardedKeys.contains(new DiscardKey(s.getId(), cursor)))
					continue;

				items.add(new ScheduleOccurrenceDto(
					s.getId(),
					cursor,
					s.getStartTime(),
					s.getEndTime(),
					s.getName(),
					s.getScheduleColor().getColorCode()
				));
			}
		}

		// date 순으로 정렬. 같은 date 내에서는 startTime, scheduleId 순으로 정렬
		items.sort(
			Comparator.comparing(ScheduleOccurrenceDto::date)
				.thenComparing(ScheduleOccurrenceDto::startTime)
				.thenComparing(ScheduleOccurrenceDto::scheduleId)
		);

		return ScheduleOccurrencesResponse.of(isFireLitToday, items);
	}

	private void checkIsExistsAndAccessibleByParentIdAndChildId(Long parentId, Long childId) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}
	}
}
