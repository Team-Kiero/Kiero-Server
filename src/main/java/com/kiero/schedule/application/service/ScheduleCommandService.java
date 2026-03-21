package com.kiero.schedule.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
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
import com.kiero.schedule.application.service.resolver.ScheduleUpdateCase;
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

	private final ScheduleEventPort eventPort;
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

		validateAddAndUpdateRequest(request.isRecurring(), request.dayOfWeek(), request.dates(), request.startTime(), request.endTime(), isFireLitToday, isExistsTodayNotPendingAfterEndTime);

		if (request.isRecurring()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			LocalDate repeatStartDate = repeatStartDateResolver(true, request.firstOrderDate(), dayOfWeeks, request.startTime(), today, now);

			throwExceptionWhenAddRecurringScheduleIfDuplicated(request.dayOfWeek(), request.startTime(), request.endTime(), child.getId(), null, repeatStartDate, null);

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

		// 아이의 오늘 일정에 영향이 있을 때만 이벤트 전송
		if (isEffectsToChildSchedule) eventPort.publish(new ScheduleModifiedEvent(childId));
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
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);
		}

		scheduleDetail.changeScheduleStatus(ScheduleStatus.VERIFIED);
		scheduleDetail.changeImageUrl(request.imageUrl());

		List<Parent> parents = parentChildLoadPort.findParentsByChildId(childId);
		List<Long> parentIds = parents.stream()
			.map(Parent::getId)
			.toList();

		eventPort.publish(new NowScheduleCompleteEventForFeed(
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
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);
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

		eventPort.publish(new FireLitEvent(parentIds, child.getId()));
		eventPort.publish(new FireLitEventForFeed(parents, child.getId(), earnedCoinAmount, LocalDateTime.now(clock)));
		return FireLitResponse.of(gotStones, earnedCoinAmount);
	}

	@Override
	@Transactional
	public void updateSchedule(Long parentId, Long scheduleId, LocalDate selectedDate, ScheduleModifyRequest request) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		if (selectedDate.isBefore(today)) {
			throw new KieroException(ScheduleErrorCode.PAST_SCHEDULE_CANNOT_BE_MODIFIED);
		}

		Schedule originalSchedule = schedulePersistencePort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!parentId.equals(originalSchedule.getParent().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		Long childId = originalSchedule.getChild().getId();

		// 오늘 아이의 불피우기 완료 여부
		boolean isFireLitToday = scheduleDetailPersistencePort.existsStoneUsedTodayByChildIdAndDate(childId, LocalDate.now(clock));

		// 요청한 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
		boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(today, childId, request.endTime());

		// 오늘 일정을 수정하려고 할 때, 수정하려는 일정 상태가 PENDING이 아니거나 이미 시작된 일정이라면 예외
		if (selectedDate.isEqual(today)) {
			ScheduleDetail originalTodayDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(scheduleId, today)
				.orElseThrow(()-> new KieroException(ScheduleErrorCode.INTERNAL_SERVER_ERROR));
			if (originalTodayDetail.getScheduleStatus() != ScheduleStatus.PENDING || !originalSchedule.getStartTime().isAfter(now)) {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
			}
		}

		validateAddAndUpdateRequest(request.isRecurring(), request.dayOfWeek(), request.dates(), request.startTime(), request.endTime(), isFireLitToday, isExistsTodayNotPendingAfterEndTime);

		boolean isEffectsToChildSchedule = false;

		ScheduleUpdateCase scheduleUpdateCase = scheduleUpdateCaseResolver(originalSchedule, request);

		switch (scheduleUpdateCase) {

			/*
			단일일정 -> 단일일정일 때,
			1) 기존의 scheduleDetail을 삭제합니다.
			2) 새로운 schedule을 생성합니다.
			3) scheduleDetail을 생성합니다.

			새로 생성된 일정의 날짜가 오늘이고 일정 시작 시각이 현재보다 이후라면,
			4) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			5) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case NormalToNormal -> {

				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);

				throwExceptionWhenAddNormalScheduleIfDuplicated(request.startTime(), request.endTime(), request.dates(),
					childId, originalSchedule.getId());

				// 새로운 schedule 생성 및 저장
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

				List<LocalDate> dates = dateParser(request.dates());
				List<ScheduleDetail> details = dates.stream()
					.distinct()
					.sorted()
					.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, saved))
					.toList();

				scheduleDetailPersistencePort.saveAll(details);

				// 수정 이후 일정의 날짜에 오늘이 포함되고 유효하거나, 원본 일정이 오늘이었다면
				if ((dates.contains(today) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) || selectedDate.isEqual(today)) {
					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			/*
			단일일정 -> 반복일정일 때,
			1) 기존의 scheduleDetail을 삭제합니다.
			2) 새로운 schedule을 생성합니다.
			3) 새로운 scheduleRepeatDays를 생성합니다.

			새로 생성된 일정의 날짜가 오늘이고 일정 시작 시각이 현재보다 이후라면,
			4) scheduleDetail을 추가로 생성합니다.
			5) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			6) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case NormalToRecurring -> {
				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);

				List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());

				LocalDate repeatStartDate = repeatStartDateResolver(false, selectedDate, dayOfWeeks, request.startTime(), today, now);

				throwExceptionWhenAddRecurringScheduleIfDuplicated(request.dayOfWeek(), request.startTime(),
					request.endTime(), childId, selectedDate, repeatStartDate, originalSchedule.getId());

				// 새로운 schedule 생성 및 저장
				Schedule newSchedule = Schedule.create(
					originalSchedule.getParent(),
					originalSchedule.getChild(),
					request.name(),
					request.startTime(),
					request.endTime(),
					request.scheduleColor(),
					true,
					repeatStartDate,
					null
				);

				Schedule saved = schedulePersistencePort.save(newSchedule);

				List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
					.map(day -> ScheduleRepeatDays.create(day, saved))
					.toList();
				scheduleRepeatDaysPersistencePort.saveAll(repeatDays);

				DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

				// 추가된 일정의 요일에 오늘이 포함되고 일정 시작 시간이 현재 이후며, 아이가 오늘 불피우기를 하지 않았고 아이가 미리 수행한 일정이 없다면
				// 1) 추가된 일정의 scheduleDetail 생성
				// 2) 오늘 일정들의 stoneType 재계산
				// 3) 이벤트를 발행하도록 설정
				if (repeatStartDate.isEqual(today) && dayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {

					ScheduleDetail scheduleDetail = ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, saved);
					scheduleDetailPersistencePort.save(scheduleDetail);

					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			/*
			반복일정 -> 반복일정이고 요일 변화가 있을 때,
			1) selectedDate를 기준으로 기존의 scheduleDetail가 존재한다면 삭제합니다.
			2) 기존의 일정의 반복종료일자를 selectedDate 이전 마지막 반복일자로 업데이트합니다.
			3) request body로 새로운 schedule을 생성하고, 반복시작일자는 selectedDate로 합니다.
			4) request body와 새로 생성한 schedule로 scheduleRepeatDays를 생성합니다.

			새로 생성된 일정의 날짜가 오늘이고 일정 시작 시각이 현재보다 이후라면,
			4) scheduleDetail을 추가로 6생성합니다.
			5) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			6) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case RecurringToRecurring -> {

				List<DayOfWeek> originalDayOfWeeks = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(
					originalSchedule.getId());
				List<DayOfWeek> requestDayOfWeeks = dayOfWeekParser(request.dayOfWeek());

				LocalDate newScheduleRepeatStartDate = repeatStartDateResolver(false, selectedDate, requestDayOfWeeks, request.startTime(), today, now);

				throwExceptionWhenAddRecurringScheduleIfDuplicated(request.dayOfWeek(), request.startTime(),
					request.endTime(), childId, selectedDate, newScheduleRepeatStartDate, originalSchedule.getId());

				LocalDate originalScheduleRepeatEndDate = repeatEndDateResolver(selectedDate, originalDayOfWeeks);

				// 더 이상 유효하지 않은 반복 일정은 하드딜리트
				if (originalScheduleRepeatEndDate.isBefore(originalSchedule.getRepeatStartDate())) {
					deleteScheduleSet(originalSchedule);
				}
				else originalSchedule.changeRepeatEndDate(originalScheduleRepeatEndDate);

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

				List<ScheduleRepeatDays> scheduleRepeatDays = requestDayOfWeeks.stream()
					.map(d -> ScheduleRepeatDays.create(d, saved))
					.toList();

				scheduleRepeatDaysPersistencePort.saveAll(scheduleRepeatDays);

				DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

				// 추가된 일정의 요일에 오늘이 포함되고 일정 시작 시간이 현재 이후며, 아이가 오늘 불피우기를 하지 않았고 아이가 미리 수행한 일정이 없다면
				// 1) 오리지널 일정의 오늘자 scheduleDetail 삭제
				// 2) 추가된 일정의 scheduleDetail 생성
				// 3) 오늘 일정들의 stoneType 재계산
				// 4) 이벤트를 발행하도록 설정
				if (newScheduleRepeatStartDate.isEqual(today) && requestDayOfWeeks.contains(todayDayOfWeek) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {
					scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), today);

					ScheduleDetail scheduleDetail = ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, saved);
					scheduleDetailPersistencePort.save(scheduleDetail);

					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			/*
			반복일정 -> 반복일정이고 요일 변화가 없고 이후 일정을 포함하여 수정할 때,
			1) 기존의 일정의 반복마감일자를 selectedDate 하루 전으로 업데이트합니다.
			2) request body로 새로운 schedule을 생성하고, 반복시작일자는 selectedDate로 합니다.
			3) 기존의 일정의 반복요일과 새로 생성한 schedule로 새로 생성된 schedule의 scheduleRepeatDays를 생성합니다.

			기존의 일정의 scheduleDetail이 생성되어 있다면, (즉, 반복요일에 오늘이 해당된다면)
			4) 참조하는 schedule을 오리지널에서 새로 생성된 schedule로 변경합니다.

			요청의 일정의 startTime이 현재 시간보다 이후라면,
			6) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			7) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case RecurringToRecurringIncludeFollowing -> {

				List<DayOfWeek> dayOfWeeks = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(scheduleId);

				boolean sameDays = new HashSet<>(dayOfWeekParser(request.dayOfWeek())).equals(new HashSet<>(dayOfWeeks));
				if (!sameDays) throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_CANNOT_BE_MODIFIED);

				LocalDate newScheduleRepeatStartDate = repeatStartDateResolver(false, selectedDate, dayOfWeeks, request.startTime(), today, now);

				throwExceptionWhenAddRecurringScheduleIfDuplicated(request.dayOfWeek(), request.startTime(),
					request.endTime(), childId, selectedDate, newScheduleRepeatStartDate, originalSchedule.getId());

				LocalDate originalScheduleRepeatEndDate = repeatEndDateResolver(selectedDate, dayOfWeeks);

				// 원본 일정의 오늘자 scheduleDetail이 존재하는가 (= 오늘이 원본일정의 반복요일에 해당했는가)
				boolean isOriginalDetailExists = scheduleDetailPersistencePort.findByScheduleIdAndDate(
					originalSchedule.getId(), selectedDate).isPresent();

				// 더 이상 유효하지 않은 반복 일정은 하드딜리트
				if (originalScheduleRepeatEndDate.isBefore(originalSchedule.getRepeatStartDate())) {
					deleteScheduleSet(originalSchedule);
				}
				else originalSchedule.changeRepeatEndDate(originalScheduleRepeatEndDate);

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

				List<ScheduleRepeatDays> scheduleRepeatDays = dayOfWeeks.stream()
					.map(d -> ScheduleRepeatDays.create(d, saved))
					.toList();

				scheduleRepeatDaysPersistencePort.saveAll(scheduleRepeatDays);

				if(isOriginalDetailExists) {
					ScheduleDetail scheduleDetail = ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING,
						null, saved);
					scheduleDetailPersistencePort.save(scheduleDetail);
					if (request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {
						recalculateTodayStoneTypes(childId);
						isEffectsToChildSchedule = true;
					}
				}
			}

			/*
			반복일정 -> 반복일정이고 요일 변화가 없고 이후 일정을 포함하지 않고 수정할 때,
			1) request body로 새로운 schedule을 생성하고, isRecurring은 false로 설정합니다.

			기존의 일정의 scheduleDetail이 생성되어 있다면, (즉, 반복요일에 오늘이 해당된다면)
			2) 참조하는 schedule을 오리지널에서 새로 생성된 schedule로 변경합니다.

			요청의 일정의 startTime이 현재 시간보다 이후라면,
			3) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			4) 오늘 일정들의 불조각 종류를 재계산합니다.

			5) 오리지널 일정을 담아 discardedSchedule을 생성합니다.
			 */
			case RecurringToRecurringExceptFollowing -> {

				List<DayOfWeek> dayOfWeeks = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(scheduleId);

				boolean sameDays = new HashSet<>(dayOfWeekParser(request.dayOfWeek())).equals(new HashSet<>(dayOfWeeks));
				if (!sameDays) throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_CANNOT_BE_MODIFIED);

				throwExceptionWhenAddNormalScheduleIfDuplicated(request.startTime(), request.endTime(), selectedDate.toString(),
					childId, originalSchedule.getId());

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

				Optional<ScheduleDetail> originalDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(
					originalSchedule.getId(), selectedDate);

				if(originalDetail.isPresent()) {
					originalDetail.get().changeSchedule(saved);
					if (request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {
						recalculateTodayStoneTypes(childId);
						isEffectsToChildSchedule = true;
					}
				} else {
					ScheduleDetail scheduleDetail = ScheduleDetail.create(selectedDate, null, null, ScheduleStatus.PENDING, null, saved);
					scheduleDetailPersistencePort.save(scheduleDetail);
				}

				DiscardedSchedule discardedSchedule = DiscardedSchedule.create(selectedDate, originalSchedule);

				discardedSchedulePersistencePort.save(discardedSchedule);
			}

			/*
			반복일정 -> 단일일정일 때,
			1) 오리지널 schedule과 연관된 scheduleRepeatDays 삭제합니다.
			2) 오리지널 schedule을 request body 내용으로 업데이트합니다.
			3) 각 일자의 scheduleDetail이 생성되어 있지 않다면, 새로 scheduleDetail을 생성합니다.

			요청의 dates에 오늘이 포함되어 있고, startTime이 현재 시간보다 이후라면,
			3) 이벤트 발행을 위해 isEffectsToChildSchedule을 true로 바꿉니다.
			4) 오늘 일정들의 불조각 종류를 재계산합니다.
			 */
			case RecurringToNormal -> {

				scheduleRepeatDaysPersistencePort.deleteAllByScheduleId(scheduleId);

				throwExceptionWhenAddNormalScheduleIfDuplicated(request.startTime(), request.endTime(), request.dates(),
					childId, originalSchedule.getId());

				List<LocalDate> dates = dateParser(request.dates());

				originalSchedule.changeBasics(request.name(), request.startTime(), request.endTime(), request.scheduleColor());
				originalSchedule.convertToNormal();

				List<ScheduleDetail> scheduleDetails = dates.stream()
					.filter(d -> !scheduleDetailPersistencePort.existsByScheduleIdAndDate(scheduleId, d))
					.map(d -> ScheduleDetail.create(d, null, null, ScheduleStatus.PENDING, null, originalSchedule))
					.toList();

				scheduleDetailPersistencePort.saveAll(scheduleDetails);

				if (dates.contains(today) && request.startTime().isAfter(now) && !isFireLitToday && !isExistsTodayNotPendingAfterEndTime) {
					recalculateTodayStoneTypes(childId);
					isEffectsToChildSchedule = true;
				}
			}

			default -> throw new KieroException(ScheduleErrorCode.SCHEDULE_UPDATE_CASE_CANNOT_RESOLVED);
		}

		// 아이의 오늘 일정에 영향이 있을 때만 이벤트 전송
		if (isEffectsToChildSchedule) eventPort.publish(new ScheduleModifiedEvent(childId));
	}

	@Override
	@Transactional
	public void deleteSchedule(Long parentId, Long scheduleId, LocalDate selectedDate, ScheduleDeleteRequest request) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);

		if (selectedDate.isBefore(today)) {
			throw new KieroException(ScheduleErrorCode.PAST_SCHEDULE_CANNOT_BE_MODIFIED);
		}

		Schedule originalSchedule = schedulePersistencePort.findById(scheduleId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!parentId.equals(originalSchedule.getParent().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		Long childId = originalSchedule.getChild().getId();

		Optional<ScheduleDetail> scheduleDetail = scheduleDetailPersistencePort.findByScheduleIdAndDate(originalSchedule.getId(), selectedDate);

		boolean isToday = selectedDate.isEqual(today);
		boolean hasDetail = scheduleDetail.isPresent();
		boolean isPending = hasDetail && scheduleDetail.get().getScheduleStatus() == ScheduleStatus.PENDING;
		boolean isBeforeStart = originalSchedule.getStartTime().isAfter(now);

		// 삭제하려는 일정의 종료시간 이후에 아이가 행위를 수행한 일정이 있는지 여부
		boolean isExistsTodayNotPendingAfterEndTime = scheduleDetailPersistencePort.existsByDateAndChildIdAfterEndTime(today, childId, originalSchedule.getEndTime());

		// 삭제하려는 일정이 반복일정일 경우
		if (originalSchedule.isRecurring()) {
			if (request == null || request.isIncludeFollowing() == null)
				throw new KieroException(ScheduleErrorCode.IS_INCLUDE_FOLLOWING_IS_REQUIRED);

			if (request.isIncludeFollowing()) { // 이후 일정을 포함하는 경우

				// 기존 일정의 반복 종료일자 구하기
				List<DayOfWeek> dayOfWeeks = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(
					originalSchedule.getId());
				LocalDate repeatEndDate = repeatEndDateResolver(selectedDate, dayOfWeeks);

				// 더 이상 유효하지 않은 반복 일정은 하드딜리트
				if (repeatEndDate.isBefore(originalSchedule.getRepeatStartDate())) {
					deleteScheduleSet(originalSchedule);
				}

				// 1) 유효한 일정은 repeatEndDate 업데이트
				// 삭제 요청한 일정이 오늘이고 유효하다면
				// 2) scheduleDetail 삭제
				// 3) 이벤트 발행
				// 4) 불조각 종류 재계산
				if ( !isToday ) {
					originalSchedule.changeRepeatEndDate(repeatEndDate);
				}
				else if ( hasDetail && isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime) {
					originalSchedule.changeRepeatEndDate(repeatEndDate);

					scheduleDetailPersistencePort.deleteScheduleDetail(scheduleDetail.get());

					eventPort.publish(new ScheduleModifiedEvent(childId));
					recalculateTodayStoneTypes(childId);
				}
				else {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
				}


			} else { // 이후 일정을 포함하지 않는 경우
				if ( !isToday ) {
					DiscardedSchedule discardedSchedule = DiscardedSchedule.create(selectedDate, originalSchedule);
					discardedSchedulePersistencePort.save(discardedSchedule);
				}
				else if ( hasDetail && isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime) {
					DiscardedSchedule discardedSchedule = DiscardedSchedule.create(selectedDate, originalSchedule);
					discardedSchedulePersistencePort.save(discardedSchedule);

					scheduleDetailPersistencePort.deleteScheduleDetail(scheduleDetail.get());

					eventPort.publish(new ScheduleModifiedEvent(childId));
					recalculateTodayStoneTypes(childId);
				}
				else {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
				}
			}
		}

		// 삭제하려는 일정이 단일일정일 경우
		else {
			if ( !isToday ) {
				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);
			}
			else if ( isPending && isBeforeStart && !isExistsTodayNotPendingAfterEndTime) {
				scheduleDetailPersistencePort.deleteByScheduleIdAndDate(originalSchedule.getId(), selectedDate);

				eventPort.publish(new ScheduleModifiedEvent(childId));
				recalculateTodayStoneTypes(childId);
			}
			else {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_CANNOT_BE_MANIPULATED);
			}

		}
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

	private void validateAddAndUpdateRequest(boolean isRecurring, String dayOfWeek, String dates, LocalTime startTime, LocalTime endTime, boolean isFireLitToday, boolean isExistsTodayNotPendingAfterEndTime) {

		LocalDate today = LocalDate.now(clock);
		LocalTime now = LocalTime.now(clock);
		List<LocalDate> requestDates = dates == null ? null : dateParser(dates);

		// 반복일정일 때 dayOfWeek 필드가 비어있으면 예외
		if (isRecurring && (dayOfWeek == null || dayOfWeek.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}
		// 단일일정일 때 dates 필드가 비어있으면 예외
		if (!isRecurring && (dates == null || dates.isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
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
					throw new KieroException(ScheduleErrorCode.SCHEDULE_NOT_MANIPULATED_WHEN_FIRE_LIT);
				}
				// 이후 일정 중 아이가 행위를 진행한 일정이 있으면 예외
				if (isExistsTodayNotPendingAfterEndTime) {
					throw new KieroException(
						ScheduleErrorCode.SCHEDULE_NOT_MANIPULATED_WHEN_AFTER_SCHEDULE_NOT_PENDING);
				}
				// 현재 시각이 일정의 startTime 이후라면 예외
				if (!now.isBefore(startTime)) {
					throw new KieroException(ScheduleErrorCode.SCHEDULE_IN_PAST);
				}
			}
		}
	}

	private void throwExceptionWhenAddRecurringScheduleIfDuplicated(String dayOfWeek, LocalTime startTime, LocalTime endTime, Long childId, LocalDate selectedDate, LocalDate requestRepeatStartDate, Long excludeScheduleId) {
		List<DayOfWeek> targetDays = dayOfWeekParser(dayOfWeek);
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
		// 일정 수정을 위한 충돌 검사의 경우 (selectedDate가 not null인 경우), 추가 필터를 적용한다.
		// ㄴ selectedDate부터 반복일정의 반복이 시작되므로, selectedDate 이후 시점에 있는 단일 일정만 충돌 고려 대상으로 둔다.
		boolean conflictWithNormal = normalsFromToday.stream()
			.filter(sd -> targetDays.contains(DayOfWeek.from(sd.getDate().getDayOfWeek())))
			.filter(sd -> !discardedKeys.contains(new DiscardKey(sd.getSchedule().getId(), sd.getDate())))
			.filter(sd -> !sd.getSchedule().getId().equals(excludeScheduleId))
			.filter(sd -> selectedDate == null || sd.getDate().isAfter(selectedDate))
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

	private ScheduleUpdateCase scheduleUpdateCaseResolver(Schedule schedule, ScheduleModifyRequest request) {

		if (!schedule.isRecurring()) {
			if (!request.isRecurring()) return ScheduleUpdateCase.NormalToNormal;
			else return ScheduleUpdateCase.NormalToRecurring;
		} else {
			if (!request.isRecurring()) return ScheduleUpdateCase.RecurringToNormal;
			else {
				List<DayOfWeek> originalRepeatDays = scheduleRepeatDaysPersistencePort.findDayOfWeeksByScheduleId(schedule.getId());
				List<DayOfWeek> requestRepeatDays = dayOfWeekParser(request.dayOfWeek());
				boolean isSame = new HashSet<>(originalRepeatDays).equals(new HashSet<>(requestRepeatDays));

				if (!isSame) return ScheduleUpdateCase.RecurringToRecurring;
				else if (request.isIncludeFollowing()) return ScheduleUpdateCase.RecurringToRecurringIncludeFollowing;
				else return ScheduleUpdateCase.RecurringToRecurringExceptFollowing;
			}
		}
	}

	private void recalculateTodayStoneTypes(Long childId) {
		LocalDate today = LocalDate.now(clock);

		List<ScheduleDetail> allScheduleDetails = scheduleDetailPersistencePort.findByDateAndChildId(today, childId);
		LocalDateTime earliestStoneUsedAt = scheduleQueryService.findEarliestStoneUsedAt(allScheduleDetails);

		List<ScheduleDetail> filteredAllScheduleDetails = scheduleQueryService.filterTodayCreatedSchedules(today, allScheduleDetails, earliestStoneUsedAt);

		calculateStoneTypePerScheduleDetail(filteredAllScheduleDetails);
	}

	// 반복일정이 첫 번째로 시작되는 일자를 구하는 리졸버
	private LocalDate repeatStartDateResolver(
		boolean isAdd,
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

			if (isAdd) baseDate = selectedDate; // 일정 추가의 상황이라면
			else if (includesToday && startTime.isAfter(now)) baseDate = today; // 일정 수정의 상황이고, 반복요일에 오늘이 포함되고, 일정 시작시간이 현재보다 이후라면
			else baseDate = selectedDate; // 일정 수정의 상황이고, 반복요일에 오늘이 포함되고, 일정 시작시간이 현재보다 이전이라면

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

	// selectedDate를 기준으로, 기존 반복 요일 중 selectedDate 이전의 마지막 발생일을 반환.
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
}