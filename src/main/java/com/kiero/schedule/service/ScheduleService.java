package com.kiero.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
import com.kiero.global.infrastructure.s3.service.S3Service;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleDetail;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;
import com.kiero.schedule.domain.enums.ScheduleStatus;
import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;
import com.kiero.schedule.exception.ScheduleErrorCode;
import com.kiero.schedule.presentation.dto.NowScheduleCompleteEvent;
import com.kiero.schedule.presentation.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.presentation.dto.FireLitEvent;
import com.kiero.schedule.presentation.dto.FireLitResponse;
import com.kiero.schedule.presentation.dto.NormalScheduleDto;
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

	private final static int ALL_SCHEDULE_SUCCESS_REWARD = 10;

	@Transactional
	public TodayScheduleResponse getTodaySchedule(Long childId) {
		LocalDate today = LocalDate.now();

		// 오늘 일정들을 startTime이 이른 것부터 정렬하여 모두 가져옴
		List<ScheduleDetail> allScheduleDetails =
			scheduleDetailRepository.findByDateAndChildId(today, childId);

		List<ScheduleDetail> pendingScheduleDetails = allScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING)
			.toList();

		// 오늘 일정들의 stoneUsedAt을 통해 불피우기 시행 여부를 조사하고, 가장 빠른 불피우기 시행 시각을 추출함 (없으면 null)
		LocalDateTime earliestStoneUsedAt = findEarliestStoneUsedAt(allScheduleDetails);

		// 당일 생성된 일정들에 한해 필터를 적용함
		List<ScheduleDetail> filteredPendingScheduleDetails = filterTodayCreatedSchedules(today, pendingScheduleDetails,
			earliestStoneUsedAt);
		List<ScheduleDetail> filteredAllScheduleDetails = filterTodayCreatedSchedules(today, allScheduleDetails,
			earliestStoneUsedAt);

		// PENDING 일정 중 일정 종료 시간이 지난 일정은 일정 상태 FAILED로 변경
		markPassedSchedulesAsFailed(filteredPendingScheduleDetails);

		// 제일 먼저 진행되어야 하는 PENDING 스케쥴, 그 다음 PENDING 스케쥴
		List<ScheduleDetail> todoScheduleDetails = findTodoScheduleAndNextTodoSchedule(
			(filteredPendingScheduleDetails));
		ScheduleDetail todoScheduleDetail = todoScheduleDetails.size() > 0 ? todoScheduleDetails.get(0) : null;
		ScheduleDetail nextTodoScheduleDetail = todoScheduleDetails.size() > 1 ? todoScheduleDetails.get(1) : null;

		// 얻을 불조각 종류를 호출될 때마다 동적으로 계산함
		stoneTypeCalculateAndSetter(filteredAllScheduleDetails, todoScheduleDetail);

		// 총 일정 수, 얻은 불조각 수(인증 완료한 일정 수), 현재 일정 순서를 계산함
		int totalSchedule = filteredAllScheduleDetails.size();
		int earnedStones = (int)filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.VERIFIED)
			.count();
		boolean isSkippable = nextTodoScheduleDetail != null;

		TodayScheduleStatus todayScheduleStatus = TodayScheduleStatusResolver.resolve(
			todoScheduleDetail,
			filteredAllScheduleDetails,
			earliestStoneUsedAt
		);

		if (todoScheduleDetail == null) {
			return TodayScheduleResponse.of(
				null, null, null, null, null,
				totalSchedule,
				earnedStones,
				todayScheduleStatus,
				isSkippable
			);
		} else {
			return TodayScheduleResponse.of(
				todoScheduleDetail.getId(),
				todoScheduleDetail.getSchedule().getStartTime(),
				todoScheduleDetail.getSchedule().getEndTime(),
				todoScheduleDetail.getSchedule().getName(),
				todoScheduleDetail.getStoneType(),
				totalSchedule,
				earnedStones,
				todayScheduleStatus,
				isSkippable
			);
		}
	}

	@Transactional
	public void skipNowSchedule(Long childId, Long scheduleDetailId) {

		ScheduleDetail scheduleDetail = scheduleDetailRepository.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (!scheduleDetail.getScheduleStatus().equals(ScheduleStatus.PENDING)) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_COULD_NOT_BE_SKIPPED);
		}

		scheduleDetail.changeScheduleStatus(ScheduleStatus.SKIPPED);
	}

	@Transactional
	public void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request) {

		ScheduleDetail scheduleDetail = scheduleDetailRepository.findById(scheduleDetailId)
			.orElseThrow(() -> new KieroException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

		if (!childId.equals(scheduleDetail.getSchedule().getChild().getId())) {
			throw new KieroException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
		}

		if (scheduleDetail.getScheduleStatus().equals(ScheduleStatus.VERIFIED)) {
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
			LocalDateTime.now()
		));
	}

	@Transactional
	public FireLitResponse fireLit(Long childId) {
		LocalDate today = LocalDate.now();

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

		int totalSchedule = filteredAllScheduleDetails.size();

		List<StoneType> gotStones = filteredAllScheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus().equals(ScheduleStatus.VERIFIED))
			.map(ScheduleDetail::getStoneType)
			.toList();

		LocalDateTime now = LocalDateTime.now();
		filteredAllScheduleDetails.forEach(sd -> sd.changeStoneUsedAt(now));

		int gotStonesCount = gotStones.size();
		int earnedCoinAmount = 0;

		if (totalSchedule == gotStonesCount) {
			child.addCoin(ALL_SCHEDULE_SUCCESS_REWARD);
			earnedCoinAmount = ALL_SCHEDULE_SUCCESS_REWARD;
		}

		eventPublisher.publishEvent(new FireLitEvent(
			child.getId(),
			earnedCoinAmount,
			LocalDateTime.now()
		));

		return FireLitResponse.of(gotStones, earnedCoinAmount);
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
			&& request.dayOfWeek() != null
			&& !request.dayOfWeek().isBlank()) {

			List<DayOfWeek> dayOfWeeks = dayOfWeekParser(request.dayOfWeek());
			List<ScheduleRepeatDays> repeatDays = dayOfWeeks.stream()
				.map(day -> ScheduleRepeatDays.create(day, savedSchedule))
				.toList();

			scheduleRepeatDaysRepository.saveAll(repeatDays);
		}

		if (request.date() != null) {
			ScheduleDetail scheduleDetail = ScheduleDetail.create(request.date(), null, null, ScheduleStatus.PENDING,
				null, savedSchedule);
			scheduleDetailRepository.save(scheduleDetail);
		}
	}

	@Transactional
	public ScheduleTabResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId) {

		Parent parent = parentRepository.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));
		Child child = childRepository.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		if (!parentChildRepository.existsByParentAndChild(parent, child)) {
			throw new KieroException(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		List<Schedule> schedules = scheduleRepository.findAllByChildId(childId);
		if (schedules.isEmpty())
			return ScheduleTabResponse.of(false, List.of(), List.of());

		List<Long> scheduleIds = schedules.stream()
			.map(Schedule::getId)
			.toList();

		boolean isFireLitToday = scheduleDetailRepository.existsStoneUsedToday(scheduleIds, LocalDate.now());

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
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
		DayOfWeek customDayOfWeek = DayOfWeek.valueOf(today.getDayOfWeek().name().substring(0, 3));

		log.info("customDayOfWeek: " + customDayOfWeek + "today: " + today);

		List<Schedule> schedules = scheduleRepeatDaysRepository.findSchedulesToCreateTodayDetail(customDayOfWeek, today);
		List<ScheduleDetail> scheduleDetails = schedules.stream()
			.map(schedule -> ScheduleDetail.create(today, null, null, ScheduleStatus.PENDING, null, schedule))
			.toList();

		scheduleDetailRepository.saveAll(scheduleDetails);
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
				case 1 -> todoSchedule.changeStoneType(StoneType.WISDOM);
				case 2 -> todoSchedule.changeStoneType(StoneType.GRIT);
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

	private void markPassedSchedulesAsFailed(List<ScheduleDetail> scheduleDetails) {
		LocalTime now = LocalTime.now();
		scheduleDetails.stream()
			.filter(sd -> sd.getSchedule().getEndTime().isBefore(now))
			.forEach(sd -> sd.changeScheduleStatus(ScheduleStatus.FAILED));
	}

	private List<ScheduleDetail> findTodoScheduleAndNextTodoSchedule(List<ScheduleDetail> scheduleDetails) {
		return scheduleDetails.stream()
			.filter(sd -> sd.getScheduleStatus() == ScheduleStatus.PENDING)
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
}
