package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildAccessPort;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.schedule.application.dto.FireLitEvent;
import com.kiero.schedule.application.dto.FireLitEventForFeed;
import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteEventForFeed;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.ScheduleCacheEvent;
import com.kiero.schedule.application.dto.ScheduleDeleteRequest;
import com.kiero.schedule.application.dto.ScheduleModifiedEvent;
import com.kiero.schedule.application.dto.ScheduleModifyRequest;
import com.kiero.schedule.application.dto.ScheduleStatusUpdatedEvent;
import com.kiero.schedule.application.exception.ScheduleErrorCode;
import com.kiero.schedule.application.port.in.ScheduleCommandUseCase;
import com.kiero.schedule.application.port.out.DiscardedSchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleDetailPersistencePort;
import com.kiero.schedule.application.port.out.ScheduleEventPort;
import com.kiero.schedule.application.port.out.SchedulePersistencePort;
import com.kiero.schedule.application.port.out.ScheduleRepeatDaysPersistencePort;
import com.kiero.schedule.domain.DiscardedSchedule;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.vo.DiscardKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleCommandService implements ScheduleCommandUseCase {

	private static final int ALL_SCHEDULE_SUCCESS_REWARD = 10;

	private final ParentLoadPort parentLoadPort;
	private final ChildLoadPort childLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	private final Clock clock;

	private final SchedulePersistencePort schedulePersistencePort;
	private final ScheduleRepeatDaysPersistencePort scheduleRepeatDaysPersistencePort;
	private final ScheduleDetailPersistencePort scheduleDetailPersistencePort;
	private final DiscardedSchedulePersistencePort discardedSchedulePersistencePort;
	private final ScheduleEventPort scheduleEventPort;
	private final ParentChildLoadPort parentChildLoadPort;

	private final ScheduleQueryService scheduleQueryService;

	@Override
	@Transactional
	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.PARENT_NOT_FOUND));
		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(ScheduleErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		boolean isEffectsToChildSchedule = false;

		// 오늘 아이의 불피우기 완료 여부
		boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId, LocalDate.now(clock));

		// 요청한 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
		boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(today, childId, request.endTime());

		validateAddScheduleRequest(request.isRecurring(), request.dayOfWeek(), request.dates(), request.startTime(), request.endTime(), isFireLitToday, isExistsTodayNotPendingAfterEndTime);

		if (request.isRecurring()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			LocalDate repeatStartDate = repeatStartDateResolver(request.firstOrderDate(), dayOfWeeks, request.startTime(), today, now);

			throwExceptionWhenAddRecurringScheduleIfDuplicated(dayOfWeeks, request.startTime(), request.endTime(), child.getId(), repeatStartDate, null);

			Schedule schedule = Schedule.create(
				parent, child,
				request.name(),
				request.startTime(),
				request.endTime(),
				request.scheduleColor(),
				true,
				repeatStartDate,
				null
			);

			Schedule saved = schedulePersistencePort.save(schedule);

			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, saved))
				.toList();
			scheduleRepeatDaysPersistencePort.saveAll(repeatDays);

			// 추가된 일정의 요일에 오늘이 포함되고 일정 시작 시간이 현재 이후며, 아이가 오늘 불피우기를 하지 않았고 아이가 미리 수행한 일정이 없다면
			// 1) 추가된 일정의 scheduleDetail 생성
			// 2) 오늘 일정들의 stoneType 재계산
			// 3) 이벤트를 발행하도록 설정
			DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

			if (repeatStartDate.isEqual(today) && dayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {

				ScheduleDetail scheduleDetail = ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, saved);
				scheduleDetailPersistencePort.save(scheduleDetail);

				recalculateTodayStoneTypes(childId);
				isEffectsToChildSchedule = true;
			}

		} else {

			throwExceptionWhenAddNormalScheduleIfDuplicated(request.startTime(), request.endTime(), request.dates(), child.getId(), null);

			Schedule schedule = Schedule.create(
				parent, child,
				request.name(),
				request.startTime(),
				request.endTime(),
				request.scheduleColor(),
				false,
				null,
				null
			);

			Schedule saved = schedulePersistencePort.save(schedule);

			List<LocalDate> dates = dateParser(request.dates());
			List<ScheduleDetail> details = dates.stream()
				.distinct()
				.sorted()
				.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
				.toList();
			scheduleDetailPersistencePort.saveAll(details);

			// 추가된 일정의 요일에 오늘이 포함되고 일정 시작 시간이 현재 이후며, 아이가 오늘 불피우기를 하지 않았고 아이가 미리 수행한 일정이 없다면
			// 1) 오늘 일정들의 stoneType 재계산
			// 2) 이벤트를 발행하도록 설정
			if (dates.contains(today) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {
				recalculateTodayStoneTypes(childId);
				isEffectsToChildSchedule = true;
			}
		}

		// 아이의 오늘 일정에 영향이 있을 때만 SSE 이벤트 전송
		if (isEffectsToChildSchedule) scheduleEventPort.publish(new ScheduleModifiedEvent(childId));

		scheduleEventPort.publish(new ScheduleCacheEvent(childId));
	}


	@Override
	@Transactional
	public void skipNowSchedule(Long childId, Long scheduleDetailId) {
		childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		ScheduleDetail scheduleDetail = scheduleDetailPersistencePort.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (scheduleDetail.getScheduleStatus() == ScheduleStatus.PENDING) {
			scheduleDetail.changeScheduleStatus(ScheduleStatus.SKIPPED);
		} else if (scheduleDetail.getScheduleStatus() == ScheduleStatus.VERIFIED) {
			scheduleDetail.changeScheduleStatus(ScheduleStatus.COMPLETED);
		} else {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_COULD_NOT_BE_SKIPPED);
		}

		List<Long> parentIds = parentChildLoadPort.findParentsByChildId(childId).stream()
				.map(Parent::getId)
				.toList();

		scheduleEventPort.publish(new ScheduleStatusUpdatedEvent(childId, parentIds));
	}

	@Override
	@Transactional
	public void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request) {
		ScheduleDetail scheduleDetail = scheduleDetailPersistencePort.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (scheduleDetail.getScheduleStatus() == ScheduleStatus.VERIFIED
			|| scheduleDetail.getScheduleStatus() == ScheduleStatus.COMPLETED) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ALREADY_COMPLETED);
		}

		if (scheduleDetail.getStoneUsedAt() != null) {
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETED);
		}

		scheduleDetail.changeScheduleStatus(ScheduleStatus.VERIFIED);
		scheduleDetail.changeImageUrl(request.imageUrl());

		List<Parent> parents = parentChildLoadPort.findParentsByChildId(childId);
		List<Long> parentIds = parents.stream()
			.map(Parent::getId)
			.toList();

		scheduleEventPort.publish(new NowScheduleCompleteEventForFeed(
			parents,
			scheduleDetail.getSchedule().getChild().getId(),
			scheduleDetail.getId(),
			scheduleDetail.getSchedule().getName(),
			scheduleDetail.getImageUrl(),
			LocalDateTime.now(clock)
		));

		scheduleEventPort.publish(new ScheduleStatusUpdatedEvent(childId, parentIds));
	}

	@Override
	@Transactional
	public FireLitResponse fireLit(Long childId) {
		LocalDate today = LocalDate.now(clock);

		Child child = childLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.CHILD_NOT_FOUND));

		List<ScheduleDetail> all = scheduleDetailPersistencePort.findByDateAndChildId(today, childId);

		LocalDateTime earliestStoneUsedAt = scheduleQueryService.findEarliestStoneUsedAt(all);
		if (earliestStoneUsedAt != null) {
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETED);
		}

		List<ScheduleDetail> filteredAllScheduleDetails = scheduleQueryService.filterTodayCreatedSchedules(today, all, null);

		int totalSchedule = (int) filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		List<StoneType> gotStones = filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED || sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.map(ScheduleDetail::getStoneType)
			.toList();

		LocalDateTime now = LocalDateTime.now(clock);
		filteredAllScheduleDetails.forEach(sd -> sd.changeStoneUsedAt(now));

		int gotStonesCount = gotStones.size();
		int earnedCoinAmount = 0;

		if (totalSchedule == gotStonesCount && totalSchedule != 0) {
			child.addCoin(ALL_SCHEDULE_SUCCESS_REWARD);
			earnedCoinAmount = ALL_SCHEDULE_SUCCESS_REWARD;
		}

		List<Parent> parents = parentChildLoadPort.findParentsByChildId(child.getId());
		List<Long> parentIds = parents.stream()
			.map(Parent::getId)
			.toList();

		scheduleEventPort.publish(new FireLitEvent(parentIds, child.getId()));
		scheduleEventPort.publish(new FireLitEventForFeed(parents, child.getId(), earnedCoinAmount, LocalDateTime.now(clock)));
		return FireLitResponse.of(gotStones, earnedCoinAmount);
	}

	@Override
	@Transactional
	public void updateSchedule(Long parentId, Long scheduleId, LocalDate selectedDate, LocalDate startDate,
		LocalDate endDate, ScheduleModifyRequest request) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Schedule originalSchedule = schedulePersistencePort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!parentId.equals(originalSchedule.getParent().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (!request.startTime().isBefore(request.endTime())) {
			throw new KieroException(ScheduleErrorCode.INVALID_TIME_DURATION);
		}

		Long childId = originalSchedule.getChild().getId();

		// 단일일정을 수정할 경우
		if (!originalSchedule.isRecurring()) {
			if (selectedDate == null || startDate != null || endDate != null) {
				throw new KieroException(ScheduleErrorCode.REQUIRED_PARAMS_FOR_NORMAL_SCHEDULE);
			}

			Optional<ScheduleDetail> originalScheduleDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(
				originalSchedule.getId(), selectedDate);

			boolean isTodaySchedule = selectedDate.isEqual(today);
			boolean hasDetail = originalScheduleDetail.isPresent();
			boolean isPending = hasDetail && originalScheduleDetail.get().getScheduleStatus() == ScheduleStatus.PENDING;
			boolean isBeforeStart = originalSchedule.getStartTime().isAfter(now);
			// 삭제하려는 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
			boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(
				today, childId, originalSchedule.getEndTime());
			boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId,
				LocalDate.now(clock));
			boolean isTodayScheduleCanBeManipulated =
				isTodaySchedule && hasDetail && isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime && !isFireLitToday;

			// 기존 존재하는 일정과 시간이 충돌하면 예외
			throwExceptionWhenAddNormalScheduleIfDuplicated(request.startTime(), request.endTime(),
				selectedDate.toString(),
				childId, originalSchedule.getId());

			// 오늘 이전의 일정을 수정하려고 하면 예외
			if (selectedDate.isBefore(today)) {
				throw new KieroException(ScheduleErrorCode.PAST_SCHEDULE_CANNOT_BE_MODIFIED);
			}
			// 오늘 이후의 일정을 수정하거나, 오늘 일정 중 수정 가능한 일정을 수정
			else if (selectedDate.isAfter(today) || (selectedDate.isEqual(today) && isTodayScheduleCanBeManipulated)) {

				Schedule newSchedule = Schedule.create(
					originalSchedule.getParent(),
					originalSchedule.getChild(),
					request.name(),
					request.startTime(),
					request.endTime(),
					request.scheduleColor(),
					false,
					null,
					null
				);
				Schedule saved = schedulePersistencePort.save(newSchedule);
				originalScheduleDetail.ifPresent(detail -> detail.changeSchedule(saved));

				if (selectedDate.isEqual(today)) {
					recalculateTodayStoneTypes(childId);
					scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
				}
			} else {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
			}
		}

		// 반복일정을 수정하려 할 경우
		else {
			if (selectedDate != null || startDate == null || endDate == null) {
				throw new KieroException(ScheduleErrorCode.REQUIRED_PARAMS_FOR_RECURRING_SCHEDULE);
			}

			validateWeekRange(startDate, endDate, today);

			Optional<ScheduleDetail> originalScheduleDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(
				originalSchedule.getId(), today);

			boolean hasDetail = originalScheduleDetail.isPresent();
			boolean isPending = hasDetail && originalScheduleDetail.get().getScheduleStatus() == ScheduleStatus.PENDING;
			boolean isBeforeStart = originalSchedule.getStartTime().isAfter(now);
			// 삭제하려는 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
			boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(
				today, childId, originalSchedule.getEndTime());
			boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId,
				LocalDate.now(clock));
			boolean isTodayScheduleCanBeManipulated =
				hasDetail && isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime && !isFireLitToday;

			List<DayOfWeek> repeatDays = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(
				originalSchedule.getId());

			// discard된 일정을 제외하고, 일정의 그 주에서의 반복요일을 날짜에 매핑
			List<LocalDate> repeatDates = getActiveRepeatDates(originalSchedule, repeatDays, startDate, childId);

			// 삭제되면 안되는 일정들 중, 가장 최신 일정의 일자 추출
			Optional<LocalDate> lastCannotBeDeletedDate = repeatDates.stream()
				.filter(date -> date.isBefore(today)
					|| (date.isEqual(today) && !isBeforeStart)
					|| (date.isEqual(today) && isExistsTodayNotPendingAfterEndTime)
					|| (date.isEqual(today) && !isPending))
				.sorted()
				.reduce((first, second) -> second);

			// 기존 일정의 반복 종료 일자 계산
			LocalDate repeatEndDate = lastCannotBeDeletedDate
				.orElseGet(() -> repeatEndDateResolver(startDate, repeatDays));

			// 기존 일정의 반복 종료 일자 다음날부터, 첫 번째 반복 요일 일자
			LocalDate newScheduleRepeatStartDate = repeatStartDateResolver(repeatEndDate.plusDays(1), repeatDays,
				request.startTime(), today, now);

			// 기존 존재하는 일정과 시간이 충돌하면 예외
			throwExceptionWhenAddRecurringScheduleIfDuplicated(repeatDays, request.startTime(), request.endTime(),
				childId, newScheduleRepeatStartDate, originalSchedule.getId());

			boolean isEffectsToChildSchedule = false;

			// repeatEndDate가 repeatStartDate보다 이전이면 일정 전체가 무효 → 전체 삭제
			if (repeatEndDate.isBefore(originalSchedule.getRepeatStartDate())) {
				if (repeatDates.contains(today)) {
					isEffectsToChildSchedule = true;
					recalculateTodayStoneTypes(childId);
				}
				deleteScheduleSet(originalSchedule);
			}

			// 일정 종료일자 업데이트
			originalSchedule.changeRepeatEndDate(repeatEndDate);

			Schedule newSchedule = Schedule.create(
				originalSchedule.getParent(),
				originalSchedule.getChild(),
				request.name(),
				request.startTime(),
				request.endTime(),
				request.scheduleColor(),
				true,
				newScheduleRepeatStartDate,
				null
			);

			Schedule saved = schedulePersistencePort.save(newSchedule);

			List<ScheduleRepeatDays> scheduleRepeatDays = repeatDays.stream()
				.map(rd -> ScheduleRepeatDays.create(rd, saved))
				.toList();

			scheduleRepeatDaysPersistencePort.saveAll(scheduleRepeatDays);

			// 오늘 일정이 수정 대상이라면, 오리지널 scheduleDetail을 삭제하고 새로운 scheduleDetail 생성하여 저장
			if (repeatDates.contains(today)) {
				if (isTodayScheduleCanBeManipulated) {
					scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), today);

					ScheduleDetail scheduleDetail = ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, saved);
					scheduleDetailPersistencePort.save(scheduleDetail);

					isEffectsToChildSchedule = true;
					recalculateTodayStoneTypes(childId);
				}
			}

			if (isEffectsToChildSchedule) {
				scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
			}
		}
		scheduleEventPort.publish(new ScheduleCacheEvent(childId));
	}

	@Override
	@Transactional
	public void deleteSchedule(Long parentId, Long scheduleId, LocalDate startDate, LocalDate endDate, LocalDate selectedDate, ScheduleDeleteRequest request) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		Schedule originalSchedule = schedulePersistencePort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!parentId.equals(originalSchedule.getParent().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		Long childId = originalSchedule.getChild().getId();

		Optional<ScheduleDetail> scheduleDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(originalSchedule.getId(), today);

		boolean hasDetail = scheduleDetail.isPresent();
		boolean isPending = hasDetail && scheduleDetail.get().getScheduleStatus() == ScheduleStatus.PENDING;
		boolean isBeforeStart = originalSchedule.getStartTime().isAfter(now);
		// 삭제하려는 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
		boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(today, childId, originalSchedule.getEndTime());
		boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId, LocalDate.now(clock));
		boolean isTodayScheduleCanBeManipulated = hasDetail && isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime && !isFireLitToday;

		// 삭제하려는 일정이 반복일정인 경우
		if (originalSchedule.isRecurring()) {

			// 필수 필드가 입력되어 있지 않으면 예외
			if (request == null || request.isIncludeFollowing() == null || startDate == null || endDate == null || selectedDate != null) {
				throw new KieroException(ScheduleErrorCode.REQUIRED_PARAMS_FOR_RECURRING_SCHEDULE);
			}

			validateWeekRange(startDate, endDate, today);

			List<DayOfWeek> repeatDays = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(originalSchedule.getId());

			// discard된 일정을 제외하고, 일정의 그 주에서의 반복요일을 날짜에 매핑
			List<LocalDate> repeatDates = getActiveRepeatDates(originalSchedule, repeatDays, startDate, childId);

			// 이후 일정을 포함하는 경우
			if (request.isIncludeFollowing()) {

				// 삭제되면 안되는 일정들 중, 가장 최신 일정의 일자 추출
				Optional<LocalDate> lastCannotBeDeletedDate = repeatDates.stream()
					.filter(date -> date.isBefore(today)
						|| (date.isEqual(today) && !isBeforeStart)
						|| (date.isEqual(today) && isExistsTodayNotPendingAfterEndTime)
						|| (date.isEqual(today) && !isPending))
					.sorted()
					.reduce((first, second) -> second);

				LocalDate repeatEndDate = lastCannotBeDeletedDate
					.orElseGet(() -> repeatEndDateResolver(startDate, repeatDays));

				// repeatEndDate가 repeatStartDate보다 이전이면 일정 전체가 무효 → 전체 삭제
				if (repeatEndDate.isBefore(originalSchedule.getRepeatStartDate())) {
					if (repeatDates.contains(today)) {
						scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
						recalculateTodayStoneTypes(childId);
					}
					deleteScheduleSet(originalSchedule);
					return;
				}

				// 일정 종료일자 업데이트
				originalSchedule.changeRepeatEndDate(repeatEndDate);

				// 오늘 일정이 삭제 가능한 상태라면 scheduleDetail 삭제
				if (repeatDates.contains(today)) {
					if (isTodayScheduleCanBeManipulated) {
						scheduleDetailPersistencePort.deleteScheduleDetail(scheduleDetail.get());
						scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
						recalculateTodayStoneTypes(childId);
					}
				}
			}

			// 이번 주차만 삭제하는 경우
			else {
				// 반복 일자 중 삭제되면 안되는 일정들 제외 (삭제해도 되는 일정만 추출)
				List<LocalDate> canBeDeletedSchedules = repeatDates.stream()
					.filter(date -> date.isAfter(today)
						|| (date.isEqual(today) && isBeforeStart && !isExistsTodayNotPendingAfterEndTime && isPending))
					.toList();

				List<DiscardedSchedule> discardedSchedules = canBeDeletedSchedules.stream()
					.map(date -> DiscardedSchedule.create(date, originalSchedule))
					.toList();
				discardedSchedulePersistencePort.saveAll(discardedSchedules);

				// 오늘 일정이 삭제 가능한 상태라면 scheduleDetail 삭제
				if (repeatDates.contains(today)) {
					if (isTodayScheduleCanBeManipulated) {
						scheduleDetailPersistencePort.deleteScheduleDetail(scheduleDetail.get());
						scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
						recalculateTodayStoneTypes(childId);
					}
				}
			}
		}

		// 삭제하려는 일정이 단일일정일 경우
		else {
			if (selectedDate == null || startDate != null || endDate != null || (request != null && request.isIncludeFollowing() != null)) {
				throw new KieroException(ScheduleErrorCode.REQUIRED_PARAMS_FOR_NORMAL_SCHEDULE);
			}

			if (selectedDate.equals(today)) {
				if (isTodayScheduleCanBeManipulated) {
					scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);
					scheduleEventPort.publish(new ScheduleModifiedEvent(childId));
					recalculateTodayStoneTypes(childId);
				}
				else {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
				}
			}
			else if (selectedDate.isAfter(today)) {
				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);
			}
			else {
				throw new KieroException(ScheduleErrorCode.PAST_SCHEDULE_CANNOT_BE_MODIFIED);
			}
		}

		scheduleEventPort.publish(new ScheduleCacheEvent(childId));
	}

	protected void calculateStoneTypePerScheduleDetail(List<ScheduleDetail> scheduleDetails) {

		if (scheduleDetails.isEmpty()) return;

		Map<Long, List<ScheduleDetail>> byChild = scheduleDetails.stream()
			.collect(Collectors.groupingBy(sd -> sd.getSchedule().getChild().getId()));


		for (List<ScheduleDetail> childDetails : byChild.values()) {
			childDetails.sort(
				Comparator
					.comparing((ScheduleDetail sd) -> sd.getSchedule().getStartTime())
					.thenComparing(sd -> sd.getSchedule().getId())
			);

			for (int i = 0; i < childDetails.size(); i++) {
				ScheduleDetail sd = childDetails.get(i);

				switch (i % 3) {
					case 0 -> sd.changeStoneType(StoneType.COURAGE);
					case 1 -> sd.changeStoneType(StoneType.GRIT);
					case 2 -> sd.changeStoneType(StoneType.WISDOM);
				}
			}
		}
	}

	private void validateAddScheduleRequest(boolean isRecurring, String dayOfWeek, String dates, LocalTime startTime, LocalTime endTime, boolean isFireLitToday, boolean isExistsTodayNotPendingAfterEndTime) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);
		List<LocalDate> requestDates = dates == null ? null : dateParser(dates);

		// 반복일정일 때 dayOfWeek 필드가 비어있으면 예외
		if (isRecurring && (dayOfWeek == null || dayOfWeek.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}
		// 단일일정일 때 dates 필드가 비어있으면 예외
		if (!isRecurring && (dates == null || dates.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_RECURRING_IS_FALSE);
		}
		// dayOfWeek 혹은 dates 필드가 모두 비어있으면 예외
		if (dayOfWeek != null && dates != null) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}
		// startTime이 endTime의 이전이 아니면 예외
		if (!startTime.isBefore(endTime)) {
			throw new KieroException(ScheduleErrorCode.INVALID_TIME_DURATION);
		}
		// 단일일정일 때
		if (dates != null) {
			// 요청 날짜 중 하나라도 오늘 이전이면 예외
			if (requestDates.stream().anyMatch(date -> date.isBefore(today))) {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_IN_PAST);
			}
			// 일정 일자가 오늘을 포함하고 있을 때
			if (requestDates.contains(today)) {
				// 불피우기를 이미 완료하였다면 예외
				if (isFireLitToday) {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED_WHEN_FIRE_LIT);
				}
				// 이후 일정 중 아이가 행위를 진행한 일정이 있으면 예외
				if (isExistsTodayNotPendingAfterEndTime) {
					throw new KieroException(
						ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED_WHEN_AFTER_SCHEDULE_NOT_PENDING);
				}
				// 현재 시각이 일정의 startTime 이후라면 예외
				if (!now.isBefore(startTime)) {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_IN_PAST);
				}
			}
		}
	}

	private void throwExceptionWhenAddRecurringScheduleIfDuplicated(List<DayOfWeek> targetDays, LocalTime startTime, LocalTime endTime, Long childId, LocalDate requestRepeatStartDate, Long excludeScheduleId) {
		List<Schedule> existingRecurring = scheduleRepeatDaysPersistencePort.findSchedulesByChildIdAndDayOfWeeks(childId, targetDays);

		// 기존의 반복일정과 충돌하는지 검사
		boolean conflictWithRecurring = existingRecurring.stream()
			.filter(s -> !s.getId().equals(excludeScheduleId))
			.filter(s ->
				s.getRepeatEndDate() == null
					|| !s.getRepeatEndDate().isBefore(requestRepeatStartDate)
			)
			.anyMatch(s -> isTimeOverlapped(startTime, endTime, s.getStartTime(), s.getEndTime()));

		if (conflictWithRecurring) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

		LocalDate today = LocalDate.now(clock);
		List<ScheduleDetail> normalsFromToday = scheduleDetailPersistencePort.findAllByScheduleChildIdAndDateGreaterThanEqual(childId, today);
		List<DiscardedSchedule> discardedSchedules = discardedSchedulePersistencePort.findAllByChildIdAndDayOfWeekIn(childId, targetDays);

		Set<DiscardKey> discardedKeys = discardedSchedules.stream()
			.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
			.collect(Collectors.toSet());

		// 기존의 단일일정과 충돌하는지 검사
		boolean conflictWithNormal = normalsFromToday.stream()
			.filter(sd -> targetDays.contains(DayOfWeek.from(sd.getDate().getDayOfWeek())))
			.filter(sd -> !discardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
			.filter(sd -> !sd.getSchedule().getId().equals(excludeScheduleId))
			.filter(sd -> sd.getDate().isEqual(requestRepeatStartDate) || sd.getDate().isAfter(requestRepeatStartDate))
			.anyMatch(sd -> isTimeOverlapped(
				startTime, endTime,
				sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
			));

		if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
	}

	private void throwExceptionWhenAddNormalScheduleIfDuplicated(LocalTime startTime, LocalTime endTime, String requestDates, Long childId, Long excludeScheduleId) {
		// 새로 추가하려는 일정이 단일일정일 때
		List<LocalDate> dates = dateParser(requestDates);
		List<ScheduleDetail> thatDayDetails = scheduleDetailPersistencePort.findByDateInAndChildId(dates, childId);
		List<DiscardedSchedule> discardedSchedules = discardedSchedulePersistencePort.findAllByChildIdAndDateIn(childId, dates);

		Set<DiscardKey> discardedKeys = discardedSchedules.stream()
			.map(ds -> new DiscardKey(ds.getSchedule().getId(), ds.getDate()))
			.collect(Collectors.toSet());

		// 기존의 단일일정과 충돌하는지 검사
		boolean conflictWithNormal = thatDayDetails.stream()
			.filter(sd -> !discardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
			.filter(sd -> !sd.getSchedule().getId().equals(excludeScheduleId)) // 일정 추가 시에는 무시되는 필터, 일정 수정 시 자기 자신은 충돌검사에서 제외
			.anyMatch(sd -> isTimeOverlapped(
				startTime, endTime,
				sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
			));

		if (conflictWithNormal) throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);

		// 기존의 반복일정과 충돌하는지 검사
		for (LocalDate date : dates) {
			DayOfWeek dow = DayOfWeek.from(date.getDayOfWeek());

			List<Schedule> recurringThatDay =
				scheduleRepeatDaysPersistencePort.findSchedulesByChildIdAndDayOfWeekIn(childId, List.of(dow));

			boolean conflictWithRecurringOnThisDate = recurringThatDay.stream()
				.filter(s -> !discardedKeys.contains(new DiscardKey(s.getId(), date)))
				.filter(s -> !s.getId().equals(excludeScheduleId)) // 일정 추가 시에는 무시되는 필터, 일정 수정 시 자기 자신은 충돌검사에서 제외
				.filter(s -> s.getRepeatStartDate() != null && !s.getRepeatStartDate().isAfter(date))
				.filter(s -> s.getRepeatEndDate() == null || !s.getRepeatEndDate().isBefore(date))
				.anyMatch(s -> isTimeOverlapped(startTime, endTime, s.getStartTime(), s.getEndTime()));

			if (conflictWithRecurringOnThisDate) {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
			}
		}
	}

	private boolean isTimeOverlapped(LocalTime newStart, LocalTime newEnd, LocalTime oldStart, LocalTime oldEnd) {
		return newStart.isBefore(oldEnd) && newEnd.isAfter(oldStart);
	}

	private List<LocalDate> dateParser(String dates) {
		try {
			return Stream.of(dates.split(","))
				.map(String::trim)
				.map(LocalDate::parse)
				.distinct()
				.sorted()
				.toList();
		} catch (DateTimeParseException e) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_FORMAT);
		}
	}

	private List<DayOfWeek> dayOfWeekParser(String dayOfWeek) {
		try {
			return Stream.of(dayOfWeek.split(","))
				.map(String::trim)
				.map(String::toUpperCase)
				.map(DayOfWeek::valueOf)
				.toList();
		} catch (IllegalArgumentException e) {
			throw new KieroException(ScheduleErrorCode.INVALID_DAY_OF_WEEK);
		}
	}

	private void recalculateTodayStoneTypes(Long childId) {
		LocalDate today = LocalDate.now(clock);

		List<ScheduleDetail> allScheduleDetails = scheduleDetailPersistencePort.findByDateAndChildId(today, childId);
		LocalDateTime earliestStoneUsedAt = scheduleQueryService.findEarliestStoneUsedAt(allScheduleDetails);

		List<ScheduleDetail> filteredAllScheduleDetails = scheduleQueryService.filterTodayCreatedSchedules(today, allScheduleDetails, earliestStoneUsedAt);

		calculateStoneTypePerScheduleDetail(filteredAllScheduleDetails);
	}

	private void validateWeekRange(LocalDate startDate, LocalDate endDate, LocalDate today) {
		if (!startDate.isBefore(endDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}
		if (endDate.isBefore(today)) {
			throw new KieroException(ScheduleErrorCode.PAST_SCHEDULE_CANNOT_BE_MODIFIED);
		}
		if (startDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
			throw new KieroException(ScheduleErrorCode.INVALID_WEEK_START_DATE);
		}
		if (!endDate.equals(startDate.plusDays(6))) {
			throw new KieroException(ScheduleErrorCode.INVALID_WEEK_END_DATE);
		}
	}

	// 반복일정이 첫 번째로 시작되는 일자를 구하는 리졸버
	private LocalDate repeatStartDateResolver(
		LocalDate selectedDate,
		List<DayOfWeek> repeatDays,
		LocalTime startTime,
		LocalDate today,
		LocalTime now
	) {

		LocalDate baseDate;

		// 요청의 반복 요일에 오늘이 포함되는지 여부
		boolean includesToday = repeatDays.contains(DayOfWeek.from(today.getDayOfWeek()));

		if (selectedDate.isAfter(today)) { // 진입 일자가 미래 일자면

			baseDate = selectedDate;

		} else if (selectedDate.isBefore(today)) {

			// 반복 요일에 오늘이 포함되고, 일정 시작 시간이 현재보다 이후라면 baseDate는 오늘, 아니면 내일
			if (includesToday && startTime.isAfter(now)) baseDate = today;
			else baseDate = today.plusDays(1);

		} else { // 진입 일자가 오늘이면
			// 반복 요일에 오늘이 포함되고, 일자의 시작 시간이 현재 시간 이후라면 baseDate는 오늘
			if (includesToday && startTime.isAfter(now)) baseDate = today;

			// 그렇지 않다면 baseDate는 내일
			else baseDate = today.plusDays(1);
		}

		// baseDate가 속한 주의 월요일
		LocalDate monday = baseDate.with(java.time.DayOfWeek.MONDAY);

		// baseDate 이후로 일정의 반복 요일 중 가장 빠른 요일의 날짜
		return repeatDays.stream()
			.map(DayOfWeek::toJavaDayOfWeek)
			.map(dow -> monday.plusDays(dow.getValue() - java.time.DayOfWeek.MONDAY.getValue()))
			.map(dateInWeek -> dateInWeek.isBefore(baseDate) ? dateInWeek.plusWeeks(1) : dateInWeek)
			.min(LocalDate::compareTo)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE));
	}

	// 기존 반복 요일 중 특정 날짜 이전의 마지막 발생일을 반환.
	private LocalDate repeatEndDateResolver(LocalDate selectedDate, List<DayOfWeek> originalRepeatDays) {
		LocalDate cursor = selectedDate.minusDays(1);
		for (int i = 0; i < 7; i++) {
			DayOfWeek dow = DayOfWeek.from(cursor.getDayOfWeek());
			if (originalRepeatDays.contains(dow)) return cursor;
			cursor = cursor.minusDays(1);
		}
		return selectedDate.minusDays(1);
	}

	private void deleteScheduleSet(Schedule originalSchedule) {
		scheduleDetailPersistencePort.deleteAllByScheduleId(originalSchedule.getId());
		scheduleRepeatDaysPersistencePort.deleteAllByScheduleId(originalSchedule.getId());
		schedulePersistencePort.deleteById(originalSchedule.getId());
		discardedSchedulePersistencePort.deleteByScheduleId(originalSchedule.getId());
	}

	private List<LocalDate> getActiveRepeatDates(Schedule schedule, List<DayOfWeek> repeatDays, LocalDate startDate, Long childId) {

		// 일정의 반복요일과 대응하는 LocalDate
		List<LocalDate> allRepeatDates = repeatDays.stream()
			.map(day -> startDate.with(DayOfWeek.toJavaDayOfWeek(day)))
			.toList();

		// discarded된 날짜 제외
		Set<LocalDate> discardedDates = discardedSchedulePersistencePort
			.findAllByChildIdAndDateIn(childId, allRepeatDates)
			.stream()
			.filter(ds -> ds.getSchedule().getId().equals(schedule.getId()))
			.map(DiscardedSchedule::getDate)
			.collect(Collectors.toSet());

		return allRepeatDates.stream()
			.filter(date -> !discardedDates.contains(date))
			.toList();
	}
}