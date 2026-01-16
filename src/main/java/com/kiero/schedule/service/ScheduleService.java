package com.kiero.schedule.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleColor;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;
import com.kiero.schedule.exception.ScheduleErrorCode;
import com.kiero.schedule.presentation.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.presentation.dto.FireLitEvent;
import com.kiero.schedule.presentation.dto.FireLitResponse;
import com.kiero.schedule.presentation.dto.NormalScheduleDto;
import com.kiero.schedule.presentation.dto.NowScheduleCompleteEvent;
import com.kiero.schedule.presentation.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.presentation.dto.RecurringScheduleDto;
import com.kiero.schedule.presentation.dto.ScheduleAddRequest;
import com.kiero.schedule.presentation.dto.ScheduleTabResponse;
import com.kiero.schedule.presentation.dto.TodayScheduleResponse;
import com.kiero.schedule.repository.ScheduleDetailRepository;
import com.kiero.schedule.repository.ScheduleRepeatDaysRepository;
import com.kiero.schedule.repository.ScheduleRepository;
import com.kiero.schedule.service.resolver.TodayScheduleStatusResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

	private final ParentRepository parentRepository;
	private final ChildRepository childRepository;
	private final ParentChildRepository parentChildRepository;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;
	private final ScheduleDetailRepository scheduleDetailRepository;

	private final ApplicationEventPublisher eventPublisher;

	private final Clock clock;
	private final static int ALL_SCHEDULE_SUCCESS_REWARD = 10;

	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now(clock);

		// 당일 생성된 반복 일정 중 반복 요일이 오늘일 경우, 수동으로 scheduleDetail 생성
		createScheduleDetailOfTodayRecurringSchedules(today);

		// 오늘 일정들을 startTime이 이른 것부터 정렬하여 모두 가져옴
		List<ScheduleDetail> allScheduleDetails =
			scheduleDetailRepository.findByDateAndChildId(today, childId);

		// 일정 현황이 PENDING이거나 VERIFIED인 것을 가져옴
		List<ScheduleDetail> pendingAndVerifiedScheduleDetails = allScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING
				|| sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.toList();

		// 오늘 일정들의 stoneUsedAt을 통해 불피우기 시행 여부를 조사하고, 가장 빠른 불피우기 시행 시각을 추출함 (없으면 null)
		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);

		// 당일 생성된 일정들에 한해 필터를 적용함
		List<ScheduleDetail> filteredPendingAndVerifiedScheduleDetails = filterTodayCreatedSchedules(today,
			pendingAndVerifiedScheduleDetails,
			earliestStoneUsedAt);
		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails,
			earliestStoneUsedAt);

		// PENDING 일정 중 일정 종료 시간이 지난 일정은 일정 상태 FAILED로 변경
		markPassedPendingSchedulesAsFailed(filteredPendingAndVerifiedScheduleDetails);

		// VERIFIED 일정 중 일정 종료 시간이 지난 일정은 일정 상태 COMPLETE로 변경
		markPassedVerifiedSchedulesAsCompleted(filteredPendingAndVerifiedScheduleDetails);

		// 제일 먼저 진행되어야 하는 PENDING or VERIFIED 스케쥴과 그 다음 PENDING 스케쥴을 가져옴
		List<ScheduleDetail> todoScheduleDetails = findTodoScheduleAndNextTodoSchedule(
			(filteredPendingAndVerifiedScheduleDetails));
		ScheduleDetail todoScheduleDetail = todoScheduleDetails.size() > 0 ? todoScheduleDetails.get(0) : null;
		ScheduleDetail nextTodoScheduleDetail = todoScheduleDetails.size() > 1 ? todoScheduleDetails.get(1) : null;

		// 얻을 불조각 종류를 호출될 때마다 동적으로 계산함
		stoneTypeCalculateAndSetter(filteredAllScheduleDetails, todoScheduleDetail);

		// 스킵된 일정을 제외한 총 일정 수, 얻은 불조각 수(인증 완료한 일정 수), 현재 일정 순서를 계산함
		int totalSchedule = (int)filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		int scheduleOrder = 0;

		int earnedStones = (int)filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED
				|| sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
			.count();

		boolean isNowScheduleVerified;

		boolean isSkippable = nextTodoScheduleDetail != null;

		TodayScheduleStatus todayScheduleStatus = TodayScheduleStatusResolver.resolve(
			todoScheduleDetail,
			filteredAllScheduleDetails,
			earliestStoneUsedAt
		);

		if (todoScheduleDetail == null) {
			return TodayScheduleResponse.of(
				null, scheduleOrder, null, null, null, null,
				totalSchedule,
				earnedStones,
				todayScheduleStatus,
				isSkippable,
				false
			);
		} else {
			scheduleOrder = filteredAllScheduleDetails.indexOf(todoScheduleDetail) + 1;
			isNowScheduleVerified = todoScheduleDetail.getScheduleStatus() == ScheduleStatus.VERIFIED;
			return TodayScheduleResponse.of(
				todoScheduleDetail.getId(),
				scheduleOrder,
				todoScheduleDetail.getSchedule().getStartTime(),
				todoScheduleDetail.getSchedule().getEndTime(),
				todoScheduleDetail.getSchedule().getName(),
				todoScheduleDetail.getStoneType(),
				totalSchedule,
				earnedStones,
				todayScheduleStatus,
				isSkippable,
				isNowScheduleVerified
			);
		}
	}

	@Transactional
	public void skipNowSchedule(Long childId, Long scheduleDetailId) {

		childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		ScheduleDetail scheduleDetail = scheduleDetailRepository.findById(scheduleDetailId)
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
	}

	@Transactional
	public void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request) {

		ScheduleDetail scheduleDetail = scheduleDetailRepository.findById(scheduleDetailId)
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

		eventPublisher.publishEvent(new NowScheduleCompleteEvent(
			scheduleDetail.getSchedule().getChild().getId(),
			scheduleDetail.getSchedule().getName(),
			scheduleDetail.getImageUrl(),
			LocalDateTime.now(clock)
		));
	}

	@Transactional
	public FireLitResponse fireLit(Long childId) {
		LocalDate today = LocalDate.now(clock);

		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		List<ScheduleDetail> allScheduleDetails =
			scheduleDetailRepository.findByDateAndChildId(today, childId);

		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);
		if (earliestStoneUsedAt != null) {
			throw new KieroException(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);
		}

		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails,
			null);

		// 스킵된 일정을 제외하고 총 일정 수 계산
		int totalSchedule = (int)filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() != ScheduleStatus.SKIPPED)
			.count();

		List<StoneType> gotStones = filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED
				|| sd.getScheduleStatus() == ScheduleStatus.COMPLETED)
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

		eventPublisher.publishEvent(new FireLitEvent(
			child.getId(),
			earnedCoinAmount,
			LocalDateTime.now(clock)
		));

		return FireLitResponse.of(gotStones, earnedCoinAmount);
	}

	@Transactional
	public DefaultScheduleContentResponse getDefaultSchedule(Long parentId, Long childId) {

		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		ScheduleColor nextColor = scheduleRepository.findFirstByChildIdOrderByCreatedAtDesc(childId)
			.map(Schedule::getScheduleColor)
			.map(ScheduleColor::next)
			.orElse(ScheduleColor.SCHEDULE1);

		return new DefaultScheduleContentResponse(nextColor, nextColor.getColorCode());
	}

	@Transactional
	public void addSchedule(ScheduleAddRequest request, Long parentId, Long childId) {

		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		Schedule schedule = Schedule.create(parent, child, request.name(), request.startTime(), request.endTime(),
			request.scheduleColor(), request.isRecurring());
		Schedule savedSchedule = scheduleRepository.save(schedule);

		if (request.isRecurring()
			&& (request.dayOfWeek() == null || request.dayOfWeek().isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}

		if (!request.isRecurring()
			&& request.date() == null) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
		}

		if (request.dayOfWeek() != null
			&& request.date() != null) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}

		if (request.isRecurring()) {
			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, savedSchedule))
				.toList();

			scheduleRepeatDaysRepository.saveAll(repeatDays);
		}

		if (!request.isRecurring()) {
			ScheduleDetail scheduleDetail = ScheduleDetail.create(request.date(), null, null, ScheduleStatus.PENDING,
				null, savedSchedule);
			scheduleDetailRepository.save(scheduleDetail);
		}
	}

	@Transactional
	public ScheduleTabResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId) {

		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		if (startDate.isAfter(endDate) || endDate.isBefore(startDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		if (schedules.isEmpty())
			return ScheduleTabResponse.of(false, List.of(), List.of());

		List<Long> scheduleIds = schedules.stream()
			.map(Schedule::getId)
			.toList();

		boolean isFireLitToday = scheduleDetailRepository.existsStoneUsedToday(scheduleIds,
			LocalDate.now(clock));

		List<Long> recurringIds = schedules.stream()
			.filter(Schedule::isRecurring)
			.map(Schedule::getId)
			.toList();

		List<Long> normalIds = schedules.stream()
			.filter(s -> !s.isRecurring())
			.map(Schedule::getId)
			.toList();

		List<RecurringScheduleDto> recurringScheduleDtos = List.of();
		if (!recurringIds.isEmpty()) {
			List<ScheduleRepeatDays> repeatDays = scheduleRepeatDaysRepository.findAllByScheduleIdsIn(recurringIds);
			Map<Long, List<ScheduleRepeatDays>> repeatDaysByScheduleId = repeatDays.stream()
				.collect(Collectors.groupingBy(rd -> rd.getSchedule().getId()));

			recurringScheduleDtos = schedules.stream()
				.filter(Schedule::isRecurring)
				.map(schedule -> {
					List<ScheduleRepeatDays> days = repeatDaysByScheduleId.getOrDefault(schedule.getId(), List.of());

					String dayOfWeek = days.stream()
						.map(d -> d.getDayOfWeek().name())
						.sorted()
						.collect(Collectors.joining(", "));

					return new RecurringScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getScheduleColor().getColorCode(),
						dayOfWeek
					);
				})
				.toList();
		}

		List<NormalScheduleDto> normalScheduleDtos = List.of();
		if (!normalIds.isEmpty()) {
			List<ScheduleDetail> details = scheduleDetailRepository
				.findAllByScheduleIdInAndDateBetween(normalIds, startDate, endDate);

			Map<Long, Schedule> scheduleById = schedules.stream()
				.collect(Collectors.toMap(Schedule::getId, s -> s));

			normalScheduleDtos = details.stream()
				.map(detail -> {
					Schedule schedule = scheduleById.get(detail.getSchedule().getId());
					return new NormalScheduleDto(
						schedule.getStartTime(),
						schedule.getEndTime(),
						schedule.getName(),
						schedule.getScheduleColor().getColorCode(),
						detail.getDate()
					);
				})
				.toList();
		}

		return ScheduleTabResponse.of(isFireLitToday, recurringScheduleDtos, normalScheduleDtos);

	}

	@Transactional
	public void createTodayScheduleDetail() {
		LocalDate today = LocalDate.now(clock);
		DayOfWeek customDayOfWeek = DayOfWeek.valueOf(today.getDayOfWeek().name().substring(0, 3));

		List<Schedule> schedules = scheduleRepeatDaysRepository.findSchedulesToCreateTodayDetail(customDayOfWeek,
			today);
		List<ScheduleDetail> scheduleDetails = schedules.stream()
			.map(schedule -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, schedule))
			.toList();

		scheduleDetailRepository.saveAll(scheduleDetails);
	}

	/*
	솝트 데모데이 때 아이의 스케쥴 데이터를 삭제하기 위한 메서드
	 */
	@Transactional
	public void deleteSchedulesDataByParentAndChild(List<Long> childIds) {
		List<Schedule> schedules = scheduleRepository.findAllByChildIdIn(childIds);
		scheduleRepeatDaysRepository.deleteByScheduleIn(schedules);
		scheduleDetailRepository.deleteByScheduleIn(schedules);
		scheduleRepository.deleteAll(schedules);
	}
	/*
	 */

	@Transactional
	public void insertDummy(Long parentId, Long childId) {
		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		List<Schedule> schedulesToSave = List.of(
			Schedule.create(parent, child, "학교",
				LocalTime.parse("09:00:00"), LocalTime.parse("13:00:00"),
				ScheduleColor.SCHEDULE1, true),

			Schedule.create(parent, child, "돌봄 교실",
				LocalTime.parse("13:00:00"), LocalTime.parse("15:00:00"),
				ScheduleColor.SCHEDULE2, true),

			Schedule.create(parent, child, "태권도",
				LocalTime.parse("14:00:00"), LocalTime.parse("16:00:00"),
				ScheduleColor.SCHEDULE3, true),

			Schedule.create(parent, child, "태권도",
				LocalTime.parse("09:00:00"), LocalTime.parse("12:00:00"),
				ScheduleColor.SCHEDULE3, true),

			Schedule.create(parent, child, "피아노",
				LocalTime.parse("14:00:00"), LocalTime.parse("16:00:00"),
				ScheduleColor.SCHEDULE4, true),

			Schedule.create(parent, child, "피아노",
				LocalTime.parse("12:00:00"), LocalTime.parse("14:00:00"),
				ScheduleColor.SCHEDULE4, false),

			Schedule.create(parent, child, "수영 교실",
				LocalTime.parse("16:00:00"), LocalTime.parse("17:00:00"),
				ScheduleColor.SCHEDULE5, true),

			Schedule.create(parent, child, "수학",
				LocalTime.parse("18:00:00"), LocalTime.parse("19:00:00"),
				ScheduleColor.SCHEDULE2, true),

			Schedule.create(parent, child, "영어",
				LocalTime.parse("19:00:00"), LocalTime.parse("20:00:00"),
				ScheduleColor.SCHEDULE3, false)
		);

		// schedule 생성
		List<Schedule> savedSchedules = scheduleRepository.saveAll(schedulesToSave);

		Schedule s1 = savedSchedules.get(0);
		Schedule s2 = savedSchedules.get(1);
		Schedule s3 = savedSchedules.get(2);
		Schedule s4 = savedSchedules.get(3);
		Schedule s5 = savedSchedules.get(4);
		Schedule s6 = savedSchedules.get(5);
		Schedule s7 = savedSchedules.get(6);
		Schedule s8 = savedSchedules.get(7);
		Schedule s9 = savedSchedules.get(8);

		// schedule_detail 생성
		List<ScheduleDetail> details = List.of(
			ScheduleDetail.create(LocalDate.parse("2026-01-18"), null, null, ScheduleStatus.PENDING, null, s6),
			ScheduleDetail.create(LocalDate.parse("2026-01-13"), null, null, ScheduleStatus.PENDING, null, s9),
			ScheduleDetail.create(LocalDate.parse("2026-01-15"), null, null, ScheduleStatus.PENDING, null, s9)
		);
		scheduleDetailRepository.saveAll(details);

		// schedule_repeat_days 생성
		List<ScheduleRepeatDays> repeatDays = List.of(
			// (MON,TUE,WED,THU,FRI) -> 1
			ScheduleRepeatDays.create(DayOfWeek.MON, s1),
			ScheduleRepeatDays.create(DayOfWeek.TUE, s1),
			ScheduleRepeatDays.create(DayOfWeek.WED, s1),
			ScheduleRepeatDays.create(DayOfWeek.THU, s1),
			ScheduleRepeatDays.create(DayOfWeek.FRI, s1),

			// (MON,WED,SAT) -> 2
			ScheduleRepeatDays.create(DayOfWeek.MON, s2),
			ScheduleRepeatDays.create(DayOfWeek.WED, s2),
			ScheduleRepeatDays.create(DayOfWeek.SAT, s2),

			// (TUE) -> 3
			ScheduleRepeatDays.create(DayOfWeek.TUE, s3),

			// (SAT) -> 4
			ScheduleRepeatDays.create(DayOfWeek.SAT, s4),

			// (THU) -> 5
			ScheduleRepeatDays.create(DayOfWeek.THU, s5),

			// (WED,FRI,SAT) -> 7
			ScheduleRepeatDays.create(DayOfWeek.WED, s7),
			ScheduleRepeatDays.create(DayOfWeek.FRI, s7),
			ScheduleRepeatDays.create(DayOfWeek.SAT, s7),

			// (MON, WED) -> 8
			ScheduleRepeatDays.create(DayOfWeek.MON, s8),
			ScheduleRepeatDays.create(DayOfWeek.WED, s8)
		);
		scheduleRepeatDaysRepository.saveAll(repeatDays);
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

	private void stoneTypeCalculateAndSetter(List<ScheduleDetail> scheduleDetails, ScheduleDetail todoSchedule) {
		if (todoSchedule != null) {
			switch (scheduleDetails.indexOf(todoSchedule) % 3) {
				case 0 -> todoSchedule.changeStoneType(StoneType.COURAGE);
				case 1 -> todoSchedule.changeStoneType(StoneType.GRIT);
				case 2 -> todoSchedule.changeStoneType(StoneType.WISDOM);
			}
		}
	}

	private List<ScheduleDetail> filterTodayCreatedSchedules(LocalDate today, List<ScheduleDetail> scheduleDetails,
		LocalDateTime earliestStoneUsedAt) {
		return scheduleDetails.stream()
			.filter(sd -> {
				Schedule schedule = sd.getSchedule();
				LocalDateTime createdAt = schedule.getCreatedAt();

				// 오늘 생성된 일정이 아닌 경우, 필터를 적용하지 않고 유지
				if (!createdAt.toLocalDate().equals(today))
					return true;

				// 제약1: createdAt 시간이 startTime 이후면 제외
				if (createdAt.toLocalTime().isAfter(schedule.getStartTime()))
					return false;

				// 제약2: 불피우기가 완료되었고, createdAt이 불피우기를 시행한 시각 이후면 제외
				return earliestStoneUsedAt == null || !createdAt.isAfter(earliestStoneUsedAt);
			})
			.toList();
	}

	private void checkIsExistsAndAccessibleByParentIdAndChildId(Long parentId, Long childId) {
		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}
	}

	private void markPassedPendingSchedulesAsFailed(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now(clock);
		scheduleDetails.stream()
			.filter(
				sd -> sd.getSchedule().getEndTime().isBefore(now) && sd.getScheduleStatus() == ScheduleStatus.PENDING)
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.FAILED));
	}

	private void markPassedVerifiedSchedulesAsCompleted(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now(clock);
		scheduleDetails.stream()
			.filter(
				sd -> sd.getSchedule().getEndTime().isBefore(now) && sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.COMPLETED));
	}

	private List<ScheduleDetail> findTodoScheduleAndNextTodoSchedule(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING
				|| sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.limit(2)
			.toList();
	}

	private LocalDateTime findEarliestStoneUsedAt(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.map(ScheduleDetail::getStoneUsedAt)
			.filter(Objects::nonNull)
			.min(LocalDateTime::compareTo)
			.orElse(null);
	}

	private void createScheduleDetailOfTodayRecurringSchedules(LocalDate today) {
		LocalDateTime startOfToday = today.atStartOfDay();
		DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

		List<Schedule> schedules =
			scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(
				startOfToday,
				todayDayOfWeek,
				today
			);

		if (schedules.isEmpty()) {
			return;
		}

		List<ScheduleDetail> details = schedules.stream()
			.map(schedule -> ScheduleDetail.create(
				today,
				null,
				null,
				ScheduleStatus.PENDING,
				null,
				schedule
			))
			.toList();

		scheduleDetailRepository.saveAll(details);
	}
}
