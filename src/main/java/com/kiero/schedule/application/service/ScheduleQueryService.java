package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.application.dto.ScheduleDetailImageResponse;
import com.kiero.schedule.application.dto.ScheduleOccurrencesResponse;
import com.kiero.schedule.application.dto.ScheduleProgressForChildDto;
import com.kiero.schedule.application.dto.ScheduleProgressForChildResponse;
import com.kiero.schedule.application.dto.ScheduleProgressForParentDto;
import com.kiero.schedule.application.dto.TodayScheduleResponse;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleQueryUseCase;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatus;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatusResolver;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.enums.ScheduleColor;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.vo.DiscardKey;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleQueryService implements ScheduleQueryUseCase {

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	private final SchedulePersistencePort schedulePersistencePort;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;
	private final ScheduleCacheableService scheduleCacheableService;

	private final Clock clock;

	@Override
	@Transactional
	public DefaultScheduleContentResponse getDefaultSchedule(Long parentId, Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		ScheduleColor nextColor = schedulePersistencePort.findFirstByChildIdOrderByCreatedAtDesc(childId)
			.map(Schedule::getScheduleColor)
			.map(ScheduleColor::next)
			.orElse(ScheduleColor.SCHEDULE1);

		return new DefaultScheduleContentResponse(nextColor, nextColor.getColorCode());
	}

	@Override
	public ScheduleOccurrencesResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId,
		Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);
		validateScheduleRange(startDate, endDate);
		return scheduleCacheableService.getSchedules(startDate, endDate, childId);
	}

	@Override
	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now(clock);

		Set<DiscardKey> todayDiscardedKeys =
			discardedSchedulePersistencePort.findAllByChildIdAndDateBetween(childId, today, today).stream()
				.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
				.collect(Collectors.toSet());

		List<ScheduleDetail> allScheduleDetails = scheduleDetailPersistencePort.findByDateAndChildId(today, childId).stream()
			.filter(sd -> !todayDiscardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
			.sorted(Comparator
				.comparing((ScheduleDetail sd) -> sd.getSchedule().getStartTime())
				.thenComparing(sd -> sd.getSchedule().getId()))
			.toList();

		List<ScheduleDetail> pendingAndVerified = allScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING || sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.toList();

		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);

		List<ScheduleDetail> filteredPendingAndVerified = filterTodayCreatedSchedules(today, pendingAndVerified, earliestStoneUsedAt);
		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails, earliestStoneUsedAt);

		List<ScheduleDetail> todo2 = findTodoScheduleAndNextTodoSchedule(filteredPendingAndVerified);
		ScheduleDetail todo = todo2.size() > 0 ? todo2.get(0) : null;
		ScheduleDetail nextTodo = todo2.size() > 1 ? todo2.get(1) : null;

		int totalSchedule = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		int earnedStones = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.count();

		boolean isSkippable = nextTodo != null;

		TodayScheduleStatus todayScheduleStatus = TodayScheduleStatusResolver.resolve(
			earnedStones,
			todo,
			filteredAllScheduleDetails,
			earliestStoneUsedAt
		);

		if (todo == null) {
			return TodayScheduleResponse.of(
				null, 0, null, null, null, null,
				totalSchedule, earnedStones,
				todayScheduleStatus,
				isSkippable,
				false
			);
		}

		int order = filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.toList()
			.indexOf(todo) + 1;

		boolean isNowScheduleVerified = todo.getScheduleStatus() == ScheduleStatus.VERIFIED;

		return TodayScheduleResponse.of(
			todo.getId(),
			order,
			todo.getSchedule().getStartTime(),
			todo.getSchedule().getEndTime(),
			todo.getSchedule().getName(),
			todo.getStoneType(),
			totalSchedule,
			earnedStones,
			todayScheduleStatus,
			isSkippable,
			isNowScheduleVerified
		);
	}

	@Override
	public ScheduleProgressForChildResponse getScheduleTodayProgressForChild(Long childId) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Set<DiscardKey> todayDiscardedKeys =
			discardedSchedulePersistencePort.findAllByChildIdAndDateBetween(childId, today, today).stream()
				.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
				.collect(Collectors.toSet());

		List<ScheduleDetail> scheduleDetails =
			scheduleDetailPersistencePort.findByDateAndChildId(today, childId).stream()
				.filter(sd -> !todayDiscardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
				.toList();

		// 오늘 일정이 존재하지 않을 경우
		if (scheduleDetails.isEmpty()) { return new ScheduleProgressForChildResponse(0, false, List.of()); }

		// 당일 생성된 일정 중 유효한 일정만 필터링
		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(scheduleDetails);
		List<ScheduleDetail> filteredScheduleDetails = filterTodayCreatedSchedules(today, scheduleDetails, earliestStoneUsedAt);

		boolean isFireLitToday =  filteredScheduleDetails.stream().anyMatch(sd -> sd.getStoneUsedAt() != null);

		// 아이가 인증하지 않고 스킵한 일정을 제외하여 dto building
		List<ScheduleProgressForChildDto> schedules = filteredScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.map(sd -> {
				boolean isOngoing = !now.isBefore(sd.getSchedule().getStartTime()) && now.isBefore(sd.getSchedule().getEndTime());

				return new ScheduleProgressForChildDto(
					sd.getSchedule().getName(),
					sd.getSchedule().getStartTime(),
					sd.getSchedule().getEndTime(),
					isOngoing,
					sd.getStoneType(),
					sd.getScheduleStatus()
				);
			})
			.toList();

		return new ScheduleProgressForChildResponse(schedules.size(), isFireLitToday, schedules);
	}

	@Override
	public ScheduleProgressForParentDto getScheduleTodayProgressForParent(Long parentId, Long childId) {
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Set<DiscardKey> todayDiscardedKeys =
			discardedSchedulePersistencePort.findAllByChildIdAndDateBetween(childId, today, today).stream()
				.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
				.collect(Collectors.toSet());

		List<ScheduleDetail> scheduleDetails =
			scheduleDetailPersistencePort.findByDateAndChildId(today, childId).stream()
				.filter(sd -> !todayDiscardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
				.toList();

		// 오늘 일정이 존재하지 않을 경우
		if (scheduleDetails.isEmpty()) { return new ScheduleProgressForParentDto(false, List.of()); }

		// 당일 생성된 일정 중 유효한 일정만 필터링
		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(scheduleDetails);
		List<ScheduleDetail> filteredScheduleDetails = filterTodayCreatedSchedules(today, scheduleDetails, earliestStoneUsedAt);

		boolean isFireLitToday =  filteredScheduleDetails.stream().anyMatch(sd -> sd.getStoneUsedAt() != null);

		List<ScheduleProgressForParentDto.ScheduleDto> schedules = filteredScheduleDetails.stream()
			.map(sd -> {
				boolean isOngoing = !now.isBefore(sd.getSchedule().getStartTime()) && now.isBefore(sd.getSchedule().getEndTime());

				return new ScheduleProgressForParentDto.ScheduleDto(
					sd.getId(),
					sd.getSchedule().getName(),
					sd.getSchedule().getStartTime(),
					sd.getSchedule().getEndTime(),
					isOngoing,
					sd.getScheduleStatus()
				);
			})
			.toList();

		return new ScheduleProgressForParentDto(isFireLitToday, schedules);
	}

	@Override
	@Transactional
	public ScheduleDetailImageResponse getScheduleVerifyImage(Long scheduleDetailId, Long parentId) {
		ScheduleDetail scheduleDetail = scheduleDetailPersistencePort.findByIdWithSchedule(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		Long childId = scheduleDetail.getSchedule().getChild().getId();

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ScheduleErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		return new  ScheduleDetailImageResponse(scheduleDetail.getImageUrl());
	}

	// 당일 생성된 일정 중, startTime과 stone 사용 여부로 유효한 일정만 필터링하는 private method
	protected List<ScheduleDetail> filterTodayCreatedSchedules(LocalDate today, List<ScheduleDetail> scheduleDetails, LocalDateTime earliestStoneUsedAt) {
		return scheduleDetails.stream()
			.filter(sd -> {
				Schedule schedule = sd.getSchedule();
				LocalDateTime createdAt = sd.getCreatedAt();

				// 당일 생성된 일정이 아니면 통과
				if (!createdAt.toLocalDate().equals(today)) return true;

				// 일정의 생성 시각이 일정의 startTime보다 이후라면 통과하지 못함
				if (createdAt.toLocalTime().isAfter(schedule.getStartTime())) return false;

				// 불 피우기를 이후에 생성된 일정은 통과하지 못함
				return earliestStoneUsedAt == null || !createdAt.isAfter(earliestStoneUsedAt);
			})
			.toList();
	}

	protected LocalDateTime findEarliestStoneUsedAt(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.map(ScheduleDetail::getStoneUsedAt)
			.filter(Objects::nonNull)
			.min(LocalDateTime::compareTo)
			.orElse(null);
	}

	private List<ScheduleDetail> findTodoScheduleAndNextTodoSchedule(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING || sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.limit(2)
			.toList();
	}

	private void checkIsExistsAndAccessibleByParentIdAndChildId(Long parentId, Long childId) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ScheduleErrorCode.NOT_ALLOWED_TO_CHILD);
		}
	}

	private void validateScheduleRange(LocalDate startDate, LocalDate endDate) {
		LocalDate today = LocalDate.now(clock);

		// 종료일이 시작일보다 이후라면 예외
		if (endDate.isBefore(startDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		// startDate는 월요일이어야 함
		if (startDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
			throw new KieroException(ScheduleErrorCode.INVALID_WEEK_START_DATE);
		}

		// endDate는 일요일이어야 함
		if (!endDate.equals(startDate.plusDays(6))) {
			throw new KieroException(ScheduleErrorCode.INVALID_WEEK_END_DATE);
		}

		// 현재 기준 이전 12주 ~ 이후 12주 범위만 허용
		LocalDate minAllowedStartDate = today.minusWeeks(13);
		LocalDate maxAllowedEndDate = today.plusWeeks(13);

		if (startDate.isBefore(minAllowedStartDate) || endDate.isAfter(maxAllowedEndDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_SCHEDULE_RANGE);
		}
	}
}
