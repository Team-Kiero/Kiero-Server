package com.kiero.schedule.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
import com.kiero.schedule.presentation.dto.ScheduleCreatedEvent;
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
			earnedStones,
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

		if (request.isRecurring()
			&& (request.dayOfWeek() == null || request.dayOfWeek().isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}

		if (!request.isRecurring()
			&& (request.dates() == null || request.dates().isEmpty())) {
			throw new KieroException(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
		}

		if (request.dayOfWeek() != null
			&& request.dates() != null) {
			throw new KieroException(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}

		throwExceptionWhenScheduleDuplicated(request, child.getId());

		Schedule schedule = Schedule.create(parent, child, request.name(), request.startTime(), request.endTime(),
			request.scheduleColor(), request.isRecurring());
		Schedule savedSchedule = scheduleRepository.save(schedule);

		if (request.isRecurring()) {
			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, savedSchedule))
				.toList();

			scheduleRepeatDaysRepository.saveAll(repeatDays);
		}

		if (!request.isRecurring()) {
			List<LocalDate> dates = dateParser(request.dates());
			List<ScheduleDetail> details = dates.stream()
				.distinct()
				.sorted()
				.map(date -> ScheduleDetail.create(date, null, null, ScheduleStatus.PENDING, null, savedSchedule))
				.toList();

			scheduleDetailRepository.saveAll(details);
		}

		eventPublisher.publishEvent(new ScheduleCreatedEvent(
			childId,
			savedSchedule.getName()
		));
	}

	@Transactional
	public ScheduleTabResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId) {

		// 요청 유효 검사
		checkIsExistsAndAccessibleByParentIdAndChildId(parentId, childId);

		if (startDate.isAfter(endDate) || endDate.isBefore(startDate)) {
			throw new KieroException(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		// 모든 일정 조회
		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		if (schedules.isEmpty())
			return ScheduleTabResponse.of(false, List.of(), List.of());

		List<Long> scheduleIds = schedules.stream()
			.map(Schedule::getId)
			.toList();

		// 오늘 아이의 불피우기 여부
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

		// 반복일정 처리
		List<RecurringScheduleDto> recurringScheduleDtos = List.of();
		if (!recurringIds.isEmpty()) {
			// 모든 반복일정 조회
			List<ScheduleRepeatDays> repeatDays = scheduleRepeatDaysRepository.findAllByScheduleIdsIn(recurringIds);
			Map<Long, List<ScheduleRepeatDays>> repeatDaysByScheduleId = repeatDays.stream()
				.filter(rd -> {
					Schedule schedule = rd.getSchedule();

					// 반복일정의 createdAt을 기준으로 그 주의 월요일을 구함
					LocalDate createdWeekStart = schedule.getCreatedAt().toLocalDate().with(java.time.DayOfWeek.MONDAY);

					// 조회하려는 기간의 시작 주 월요일을 구함
					LocalDate queryWeekStart = startDate.with(java.time.DayOfWeek.MONDAY);

					// ( 등록된 주 <= 조회하는 주 )이면 노출
					// => 반복일정이 추가되면, 추가된 그 주 월요일부터 반복일정이 노출되고 그 이전 주에는 노출되지 않음
					return !createdWeekStart.isAfter(queryWeekStart);
				})
				.collect(Collectors.groupingBy(rd -> rd.getSchedule().getId()));

			recurringScheduleDtos = schedules.stream()
				.filter(Schedule::isRecurring)
				.map(schedule -> {
					List<ScheduleRepeatDays> days = repeatDaysByScheduleId.getOrDefault(schedule.getId(), List.of());

					if (days.isEmpty()) {
						return null;
					}

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
				.filter(Objects::nonNull)
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
	public void insertDummy(List<Long> parentIds, Long childId) {

		for (Long parentId : parentIds) {
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

				Schedule.create(parent, child, "데모데이",
					LocalTime.parse("01:00:00"), LocalTime.parse("01:10:00"),
					ScheduleColor.SCHEDULE5, false),

				Schedule.create(parent, child, "피아노",
					LocalTime.parse("14:00:00"), LocalTime.parse("16:00:00"),
					ScheduleColor.SCHEDULE4, true),

				Schedule.create(parent, child, "수영 교실",
					LocalTime.parse("16:00:00"), LocalTime.parse("17:00:00"),
					ScheduleColor.SCHEDULE5, true),

				Schedule.create(parent, child, "수학",
					LocalTime.parse("18:00:00"), LocalTime.parse("19:00:00"),
					ScheduleColor.SCHEDULE2, true),

				Schedule.create(parent, child, "영어",
					LocalTime.parse("19:00:00"), LocalTime.parse("20:00:00"),
					ScheduleColor.SCHEDULE3, false),

				Schedule.create(parent, child, "발표하기",
					LocalTime.parse("01:10:00"), LocalTime.parse("03:00:00"),
					ScheduleColor.SCHEDULE2, false)
			);

			LocalDateTime yesterday = LocalDateTime.now(clock).minusDays(1);

			// schedule 생성
			List<Schedule> savedSchedules = scheduleRepository.saveAll(schedulesToSave);

			savedSchedules.forEach(s -> s.forceCreatedAtForTest(yesterday));

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
				// 데모데이 일정 - imageUrl, ScheduleStatus.COMPLETED, stoneType COURAGE
				ScheduleDetail.create(LocalDate.parse("2026-01-23"), "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/Demoday.JPG", null, ScheduleStatus.COMPLETED, StoneType.COURAGE, s4),
				ScheduleDetail.create(LocalDate.parse("2026-01-19"), null, null, ScheduleStatus.PENDING, null, s8),
				ScheduleDetail.create(LocalDate.parse("2026-01-21"), null, null, ScheduleStatus.PENDING, null, s8),
				// 발표하기 일정 - imageUrl, ScheduleStatus.VERIFIED, stoneType GRIT
				ScheduleDetail.create(LocalDate.parse("2026-01-23"), "https://kiero-bucket.s3.ap-northeast-2.amazonaws.com/schedule/%E1%84%8E%E1%85%AC%E1%84%80%E1%85%B3%E1%86%AB%E1%84%8B%E1%85%A7%E1%86%BC+%E1%84%87%E1%85%A1%E1%86%AF%E1%84%91%E1%85%AD%E1%84%89%E1%85%A1%E1%84%8C%E1%85%B5%E1%86%AB.jpeg", null, ScheduleStatus.VERIFIED, StoneType.GRIT, s9)
			);
			scheduleDetailRepository.saveAll(details);

			// schedule_repeat_days 생성
			List<ScheduleRepeatDays> repeatDays = List.of(
				// (MON,TUE,WED,THU,FRI) -> 1
				ScheduleRepeatDays.create(DayOfWeek.MON, s1),
				ScheduleRepeatDays.create(DayOfWeek.TUE, s1),
				ScheduleRepeatDays.create(DayOfWeek.WED, s1),
				ScheduleRepeatDays.create(DayOfWeek.THU, s1),

				// (MON, WED) -> 2
				ScheduleRepeatDays.create(DayOfWeek.MON, s2),
				ScheduleRepeatDays.create(DayOfWeek.WED, s2),

				// (TUE) -> 3
				ScheduleRepeatDays.create(DayOfWeek.TUE, s3),

				// (THU) -> 5
				ScheduleRepeatDays.create(DayOfWeek.THU, s5),

				// (WED, FRI) -> 6
				ScheduleRepeatDays.create(DayOfWeek.WED, s6),

				// (MON, WED) -> 7
				ScheduleRepeatDays.create(DayOfWeek.MON, s7),
				ScheduleRepeatDays.create(DayOfWeek.WED, s7)
			);
			scheduleRepeatDaysRepository.saveAll(repeatDays);

			createTodayRecurringScheduleDetailsForDummy(savedSchedules, repeatDays);
		}
	}

	private void createTodayRecurringScheduleDetailsForDummy(List<Schedule> savedSchedules,
		List<ScheduleRepeatDays> repeatDays) {
		LocalDate today = LocalDate.now(clock);
		DayOfWeek todayDayOfWeek = DayOfWeek.from(today.getDayOfWeek());

		// repeatDays를 scheduleId -> 요일들로 매핑
		Map<Long, List<DayOfWeek>> repeatDaysByScheduleId = repeatDays.stream()
			.collect(Collectors.groupingBy(
				rd -> rd.getSchedule().getId(),
				Collectors.mapping(ScheduleRepeatDays::getDayOfWeek, Collectors.toList())
			));

		// 오늘 요일을 포함하는 반복 일정만 필터링
		List<Schedule> schedulesToCreateTodayDetail = savedSchedules.stream()
			.filter(Schedule::isRecurring)
			.filter(s -> {
				List<DayOfWeek> days = repeatDaysByScheduleId.getOrDefault(s.getId(), List.of());
				return days.contains(todayDayOfWeek);
			})
			.toList();

		if (schedulesToCreateTodayDetail.isEmpty())
			return;

		List<ScheduleDetail> details = schedulesToCreateTodayDetail.stream()
			.filter(s -> !scheduleDetailRepository.existsByScheduleIdAndDate(s.getId(), today))
			.map(s -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, s))
			.toList();

		if (!details.isEmpty()) {
			scheduleDetailRepository.saveAll(details);
		}
	}

	private void throwExceptionWhenScheduleDuplicated(ScheduleAddRequest request, Long childId) {

		// 반복일정일 경우
		if (request.isRecurring()) {
			// request의 요일에 해당하는 요일의 일정 조회 & 조회된 일정과 request 일정의 시간이 겹치면 exception
			List<DayOfWeek> targetDays = dayOfWeekParser(request.dayOfWeek());
			List<Schedule> existingRecurring = scheduleRepeatDaysRepository.findSchedulesByChildIdAndDayOfWeeks(
				childId,
				targetDays);
			boolean conflictWithRecurring = existingRecurring.stream()
				.anyMatch(
					s -> isTimeOverlapped(request.startTime(), request.endTime(), s.getStartTime(),
						s.getEndTime()));

			if (conflictWithRecurring) {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
			}

			// 오늘 이후의 일정 중에 request의 요일에 해당하는 단일 일정을 조회하고, 조회된 일정과 request 일정의 시간이 겹치면 exception
			LocalDate today = LocalDate.now(clock);
			List<ScheduleDetail> normalsFromToday = scheduleDetailRepository.findAllByScheduleChildIdAndDateGreaterThanEqual(
				childId, today);

			boolean conflictWithNormal = normalsFromToday.stream()
				.filter(sd -> {
					java.time.DayOfWeek dow = sd.getDate().getDayOfWeek();
					DayOfWeek custom = DayOfWeek.from(dow);
					return targetDays.contains(custom);
				})
				.anyMatch(sd -> isTimeOverlapped(
					request.startTime(), request.endTime(),
					sd.getSchedule().getStartTime(), sd.getSchedule().getEndTime()
				));

			if (conflictWithNormal) {
				throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
			}

			return;
		}

		// 단일 일정일 경우
		List<LocalDate> dates = dateParser(request.dates());

		// 기존의 단일 일정과 충돌하는지 검사
		// 입력된 날짜에 해당하는 scheduleDetail 조회
		List<ScheduleDetail> thatDayDetails = scheduleDetailRepository.findByDateInAndChildId(dates, childId);

		boolean conflictWithNormal = thatDayDetails.stream()
			.anyMatch(sd -> isTimeOverlapped(
				request.startTime(), request.endTime(), sd.getSchedule().getStartTime(),
				sd.getSchedule().getEndTime()
			));

		if (conflictWithNormal) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
		}

		// 기존의 반복 일정과 충돌하는지 검사
		// 입력된 날짜의 요일들 계산
		List<DayOfWeek> targetDays = dates.stream()
			.map(date -> DayOfWeek.from(date.getDayOfWeek()))
			.distinct()
			.toList();

		// 입력된 날짜의 요일들에 해당하는 schedule 조회
		List<Schedule> existingRecurringOnThatDay = scheduleRepeatDaysRepository.findSchedulesByChildIdAndDayOfWeekIn(
			childId, targetDays);

		boolean conflictWithRecurring = existingRecurringOnThatDay.stream()
			.anyMatch(s -> isTimeOverlapped(request.startTime(), request.endTime(), s.getStartTime(),
				s.getEndTime()));

		if (conflictWithRecurring) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_DUPLICATED);
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

	private void stoneTypeCalculateAndSetter(List<ScheduleDetail> scheduleDetails, ScheduleDetail
		todoSchedule) {
		if (todoSchedule != null) {
			switch (scheduleDetails.indexOf(todoSchedule) % 3) {
				case 0 -> todoSchedule.changeStoneType(StoneType.COURAGE);
				case 1 -> todoSchedule.changeStoneType(StoneType.GRIT);
				case 2 -> todoSchedule.changeStoneType(StoneType.WISDOM);
			}
		}
	}

	private List<ScheduleDetail> filterTodayCreatedSchedules(LocalDate
			today, List<ScheduleDetail> scheduleDetails,
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
				sd -> sd.getSchedule().getEndTime().isBefore(now)
					&& sd.getScheduleStatus() == ScheduleStatus.PENDING)
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.FAILED));
	}

	private void markPassedVerifiedSchedulesAsCompleted(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now(clock);
		scheduleDetails.stream()
			.filter(
				sd -> sd.getSchedule().getEndTime().isBefore(now)
					&& sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
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
