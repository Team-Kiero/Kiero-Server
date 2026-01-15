package com.kiero.schedule.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.anyInt;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

	@Mock
	ParentRepository parentRepository;
	@Mock
	ChildRepository childRepository;
	@Mock
	ParentChildRepository parentChildRepository;
	@Mock
	ScheduleRepository scheduleRepository;
	@Mock
	ScheduleRepeatDaysRepository scheduleRepeatDaysRepository;
	@Mock
	ScheduleDetailRepository scheduleDetailRepository;
	@Mock
	ApplicationEventPublisher eventPublisher;
	@Mock
	Clock clock;

	@InjectMocks
	ScheduleService scheduleService;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	LocalDate today = LocalDate.of(2026, 1, 15);

	Clock fixedClock = Clock.fixed(
		LocalDateTime.of(today, LocalTime.of(11, 30))
			.atZone(ZoneId.of("Asia/Seoul"))
			.toInstant(),
		ZoneId.of("Asia/Seoul")
	);

	// =========================
	// AddSchedule
	// =========================
	@Nested
	@DisplayName("addSchedule")
	class AddSchedule {

		@Test
		void 정상이면_저장하고_반복일정이면_scheduleRepeatDays_저장() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);
			Schedule savedSchedule = mock(Schedule.class);

			ScheduleAddRequest req = new ScheduleAddRequest("첫번째 일정", true, LocalTime.of(11, 0), LocalTime.of(11, 30),
				ScheduleColor.SCHEDULE1,
				"MON, TUE", null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			given(scheduleRepository.save(any(Schedule.class))).willReturn(savedSchedule);

			// when
			scheduleService.addSchedule(req, parentId, childId);

			// then 1: 일정 저장이 호출됨
			verify(scheduleRepository, times(1)).save(any(Schedule.class));

			// then 2: 반복 일정이면 repeatDays 저장이 호출됨
			verify(scheduleRepeatDaysRepository, times(1)).saveAll(anyList());

			// then 3: 일정 디테일 저장은 호출되지 않음
			verify(scheduleDetailRepository, never()).save(any(ScheduleDetail.class));
		}

		@Test
		void 정상이면_저장하고_단일일정이면_scheduleDetail_저장() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);
			Schedule savedSchedule = mock(Schedule.class);

			ScheduleAddRequest req = new ScheduleAddRequest("첫번째 일정", false, LocalTime.of(11, 0), LocalTime.of(11, 30),
				ScheduleColor.SCHEDULE1,
				null, LocalDate.of(2026, 1, 16));

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			given(scheduleRepository.save(any(Schedule.class))).willReturn(savedSchedule);

			// when
			scheduleService.addSchedule(req, parentId, childId);
		}

		@Test
		void 부모가_없으면_예외() {
			// given
			Long parentId = 1L;

			ScheduleAddRequest req = new ScheduleAddRequest(null, null, null, null, null, null, null);

			given(parentRepository.findById(parentId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, null))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.PARENT_NOT_FOUND);
		}

		@Test
		void 아이가_없으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, null, null, null, null, null, null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ChildErrorCode.CHILD_NOT_FOUND);
		}

		@Test
		void 자신의_아이가_아니면_접근제한() {
			// given
			Long parentId = 1L;
			Long otherChildId = 100L;

			Parent parent = mock(Parent.class);
			Child otherChild = mock(Child.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, null, null, null, null, null, null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(otherChildId)).willReturn(Optional.of(otherChild));
			given(parentChildRepository.existsByParentAndChild(parent, otherChild)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, otherChildId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		@Test
		void 요청_데이터의_dayOfWeek_data_모두_입력되었으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, true, null, null, null,
				"MON, TUE", LocalDate.of(2026, 1, 16));

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.DAY_OF_WEEK_XOR_DATE_REQUIRED);
		}

		@Test
		void 반복일정인데_dayOfWeek가_입력되지_않았으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, true, null, null, null, null, null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE);
		}

		@Test
		void 단일일정인데_date가_입력되지_않았으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, false, null, null, null, null, null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE);
		}

		@Test
		void 반복일정일때_요청_데이터의_dayOfWeek_형식이_맞지않으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			ScheduleAddRequest req = new ScheduleAddRequest(null, true, null, null, null, "월요일, TUE/ 수", null);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> scheduleService.addSchedule(req, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.INVALID_DAY_OF_WEEK);
		}
	}

	// =========================
	// CompleteNowSchedule
	// =========================
	@Nested
	@DisplayName("completeNowSchedule")
	class CompleteNowSchedule {
		@Test
		void 정상일때_VERIFIED로_변경되고_이벤트_발행() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			NowScheduleCompleteRequest req = new NowScheduleCompleteRequest("http://test-img.jpeg");

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			given(sd.getStoneUsedAt()).willReturn(null);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);

			// when
			scheduleService.completeNowSchedule(childId, scheduleDetailId, req);

			// then
			verify(sd).changeScheduleStatus(ScheduleStatus.VERIFIED);
			verify(sd).changeImageUrl("http://test-img.jpeg");

			verify(eventPublisher, times(1)).publishEvent(any(NowScheduleCompleteEvent.class));
		}

		@Test
		void childId가_다르면_접근거부_예외() {
			// given
			Long childId = 1L;
			Long otherChildId = 2L;
			Long scheduleDetailId = 10L;

			NowScheduleCompleteRequest req = new NowScheduleCompleteRequest("http://test-img.jpeg");

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			// when & then
			assertThatThrownBy(() -> scheduleService.completeNowSchedule(otherChildId, scheduleDetailId, req))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);

			verify(sd, never()).changeScheduleStatus(ScheduleStatus.VERIFIED);
			verify(sd, never()).changeImageUrl(req.imageUrl());
			verify(eventPublisher, never()).publishEvent(any(NowScheduleCompleteEvent.class));
		}

		@Test
		void 일정이_이미_VERIFIED면_인증완료_예외() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			NowScheduleCompleteRequest req = new NowScheduleCompleteRequest("http://test-img.jpeg");

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);

			// when & then
			assertThatThrownBy(() -> scheduleService.completeNowSchedule(childId, scheduleDetailId, req))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_ALREADY_COMPLETED);

			verify(sd, never()).changeScheduleStatus(ScheduleStatus.VERIFIED);
			verify(sd, never()).changeImageUrl(req.imageUrl());
			verify(eventPublisher, never()).publishEvent(any(NowScheduleCompleteEvent.class));
		}

		@Test
		void 일정이_이미_COMPLETED면_인증완료_예외() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			NowScheduleCompleteRequest req = new NowScheduleCompleteRequest("http://test-img.jpeg");

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.COMPLETED);

			// when & then
			assertThatThrownBy(() -> scheduleService.completeNowSchedule(childId, scheduleDetailId, req))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_ALREADY_COMPLETED);

			verify(sd, never()).changeScheduleStatus(ScheduleStatus.VERIFIED);
			verify(sd, never()).changeImageUrl(req.imageUrl());
			verify(eventPublisher, never()).publishEvent(any(NowScheduleCompleteEvent.class));
		}
	}

	// =========================
	// FireLit
	// =========================
	@Nested
	@DisplayName("fireLit")
	class FireLit {

		private static final ZoneId KST = ZoneId.of("Asia/Seoul");

		/**
		 * alreadyUsed 예외 케이스용: stoneUsedAt만 있으면 됨 (status/stoneType/schedule 전부 불필요)
		 */
		private ScheduleDetail mockDetailOnlyStoneUsedAt(LocalDateTime stoneUsedAt) {
			ScheduleDetail sd = mock(ScheduleDetail.class);
			given(sd.getStoneUsedAt()).willReturn(stoneUsedAt);
			return sd;
		}

		/**
		 * 정상 흐름용: filterTodayCreatedSchedules 통과를 위해 schedule.createdAt/startTime 필요
		 * + status는 totalSchedule/gotStones 계산에 쓰이므로 필요
		 * + stoneType은 VERIFIED/COMPLETED일 때만 실제로 호출되므로 해당 상태일 때만 스텁
		 */
		private ScheduleDetail mockDetailForNormalFlow(
			LocalDate today,
			ScheduleStatus status,
			StoneType stoneTypeOrNull,
			LocalDateTime stoneUsedAt
		) {
			ScheduleDetail sd = mock(ScheduleDetail.class);

			Schedule schedule = mock(Schedule.class);
			given(schedule.getCreatedAt()).willReturn(today.atStartOfDay());
			given(schedule.getStartTime()).willReturn(LocalTime.of(23, 59));
			given(sd.getSchedule()).willReturn(schedule);

			given(sd.getScheduleStatus()).willReturn(status);
			given(sd.getStoneUsedAt()).willReturn(stoneUsedAt);

			// stoneType은 VERIFIED/COMPLETED만 호출될 수 있으니 그때만 스텁
			if (stoneTypeOrNull != null) {
				given(sd.getStoneType()).willReturn(stoneTypeOrNull);
			}

			return sd;
		}

		@Test
		void 정상일때_모든_scheduleDetail에_stoneUsedAt이_세팅되고_이벤트_발행() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));

			// PENDING은 stoneType 호출 안 될 수 있으니 null
			ScheduleDetail sd1 = mockDetailForNormalFlow(today, ScheduleStatus.PENDING, null, null);
			// COMPLETED는 gotStones에 들어가므로 stoneType 필요
			ScheduleDetail sd2 = mockDetailForNormalFlow(today, ScheduleStatus.COMPLETED, StoneType.GRIT, null);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1, sd2));

			// when
			FireLitResponse response = scheduleService.fireLit(childId);

			// then 1: stoneUsedAt 세팅
			verify(sd1).changeStoneUsedAt(any(LocalDateTime.class));
			verify(sd2).changeStoneUsedAt(any(LocalDateTime.class));

			// then 2: 이벤트 발행
			verify(eventPublisher, times(1)).publishEvent(any(FireLitEvent.class));

			// then 3: VERIFIED or COMPLETED 일정에 대한 stone만 획득
			assertThat(response.gotStones()).containsExactly(StoneType.GRIT);
		}

		@Test
		void 스킵제외_전체일정이_완료면_코인10_지급되고_earnedCoinAmount가_10() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));

			ScheduleDetail sd1 = mockDetailForNormalFlow(today, ScheduleStatus.VERIFIED, StoneType.GRIT, null);
			ScheduleDetail sd2 = mockDetailForNormalFlow(today, ScheduleStatus.COMPLETED, StoneType.COURAGE, null);
			// SKIPPED는 stoneType 호출 안 될 수 있으니 null
			ScheduleDetail skippedSd = mockDetailForNormalFlow(today, ScheduleStatus.SKIPPED, null, null);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(
				List.of(sd1, sd2, skippedSd));

			// when
			FireLitResponse response = scheduleService.fireLit(childId);

			// then 1: 코인 지급
			verify(child, times(1)).addCoin(10);
			assertThat(response.earnedCoinAmount()).isEqualTo(10);

			// then 2: 이벤트 발행 amount 검증
			ArgumentCaptor<FireLitEvent> captor = ArgumentCaptor.forClass(FireLitEvent.class);
			verify(eventPublisher).publishEvent(captor.capture());
			assertThat(captor.getValue().amount()).isEqualTo(10);
		}

		@Test
		void 스킵제외_하나라도_미완료면_코인이_지급되지_않고_이벤트_발행() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));

			ScheduleDetail sd1 = mockDetailForNormalFlow(today, ScheduleStatus.PENDING, null, null);
			ScheduleDetail sd2 = mockDetailForNormalFlow(today, ScheduleStatus.COMPLETED, StoneType.COURAGE, null);
			ScheduleDetail sd3 = mockDetailForNormalFlow(today, ScheduleStatus.SKIPPED, null, null);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1, sd2, sd3));

			// when
			FireLitResponse response = scheduleService.fireLit(childId);

			// then 1: 코인 미지급
			verify(child, never()).addCoin(anyInt());
			assertThat(response.earnedCoinAmount()).isEqualTo(0);

			// then 2: 이벤트 발행 amount=0
			ArgumentCaptor<FireLitEvent> captor = ArgumentCaptor.forClass(FireLitEvent.class);
			verify(eventPublisher).publishEvent(captor.capture());
			assertThat(captor.getValue().amount()).isEqualTo(0);
		}

		@Test
		void 스킵제외_일정이_없으면_코인이_지급되지_않고_이벤트_발행() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));

			ScheduleDetail sd1 = mockDetailForNormalFlow(today, ScheduleStatus.SKIPPED, null, null);
			ScheduleDetail sd2 = mockDetailForNormalFlow(today, ScheduleStatus.SKIPPED, null, null);
			ScheduleDetail sd3 = mockDetailForNormalFlow(today, ScheduleStatus.SKIPPED, null, null);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1, sd2, sd3));

			// when
			FireLitResponse response = scheduleService.fireLit(childId);

			// then 1: 코인 미지급
			verify(child, never()).addCoin(anyInt());
			assertThat(response.earnedCoinAmount()).isEqualTo(0);

			// then 2: 이벤트 발행 amount=0
			ArgumentCaptor<FireLitEvent> captor = ArgumentCaptor.forClass(FireLitEvent.class);
			verify(eventPublisher).publishEvent(captor.capture());
			assertThat(captor.getValue().amount()).isEqualTo(0);
		}

		@Test
		void 아이가_없으면_예외() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			given(childRepository.findById(childId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.fireLit(childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ChildErrorCode.CHILD_NOT_FOUND);

			verify(eventPublisher, never()).publishEvent(any());
		}

		@Test
		void 이미_불피우기_완료된_일정이_있으면_예외() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));

			// stoneUsedAt만 있으면 earliestStoneUsedAt != null 로 예외 발생
			ScheduleDetail alreadyUsed = mockDetailOnlyStoneUsedAt(LocalDateTime.of(2026, 1, 14, 9, 0));

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(alreadyUsed));

			// when & then
			assertThatThrownBy(() -> scheduleService.fireLit(childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.FIRE_LIT_ALREADY_COMPLETE);

			verify(eventPublisher, never()).publishEvent(any());
			verify(alreadyUsed, never()).changeStoneUsedAt(any());
		}
	}

	// =========================
	// getDefaultSchedule
	// =========================
	@Nested
	@DisplayName("getDefaultSchedule")
	class GetDefaultSchedule {
		@Test
		void 정상일때_기본설정값을_반환() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);
			Schedule schedule = mock(Schedule.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			given(scheduleRepository.findFirstByChildIdOrderByCreatedAtDesc(childId)).willReturn(Optional.of(schedule));
			given(schedule.getScheduleColor()).willReturn(ScheduleColor.SCHEDULE1);

			// when
			DefaultScheduleContentResponse response = scheduleService.getDefaultSchedule(parentId, childId);

			// then
			assertThat(response.scheduleColor()).isEqualTo(ScheduleColor.SCHEDULE2);
			assertThat(response.colorCode()).isEqualTo(ScheduleColor.SCHEDULE2.getColorCode());
		}

		@Test
		void 부모가_없으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			given(parentRepository.findById(parentId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.getDefaultSchedule(parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.PARENT_NOT_FOUND);
		}

		@Test
		void 아이가_없으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.getDefaultSchedule(parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ChildErrorCode.CHILD_NOT_FOUND);
		}

		@Test
		void 자신의_아이가_아니면_접근제한() {
			// given
			Long parentId = 1L;
			Long otherChildId = 100L;

			Parent parent = mock(Parent.class);
			Child otherChild = mock(Child.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(otherChildId)).willReturn(Optional.of(otherChild));
			given(parentChildRepository.existsByParentAndChild(parent, otherChild)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> scheduleService.getDefaultSchedule(parentId, otherChildId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}
	}

	// =========================
	// SkipNowSchedule
	// =========================
	@Nested
	@DisplayName("skipNowSchedule")
	class SkipNowSchedule {
		@Test
		void 정상이면_pending인_일정을_skipped로_변경() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);

			// when
			scheduleService.skipNowSchedule(childId, scheduleDetailId);

			// then
			verify(sd).changeScheduleStatus(ScheduleStatus.SKIPPED);
		}

		@Test
		void 정상이면_verified인_일정을_complete로_변경() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);

			// when
			scheduleService.skipNowSchedule(childId, scheduleDetailId);

			// then
			verify(sd).changeScheduleStatus(ScheduleStatus.COMPLETED);
		}

		@Test
		void 아이가_없으면_예외() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			given(childRepository.findById(childId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.skipNowSchedule(childId, scheduleDetailId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ChildErrorCode.CHILD_NOT_FOUND);
		}

		@Test
		void scheduleDetail이_존재하지_않으면_예외() {
			// given
			Long childId = 1L;
			Long sdId = 10L;

			Child child = mock(Child.class);
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(scheduleDetailRepository.findById(sdId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.skipNowSchedule(childId, sdId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
		}

		@Test
		void childId가_다르면_접근거부_예외() {
			// given
			Long childId = 1L;
			Long otherChildId = 2L;
			Long scheduleDetailId = 10L;

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(childRepository.findById(otherChildId)).willReturn(Optional.of(child));
			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			// when & then
			assertThatThrownBy(() -> scheduleService.skipNowSchedule(otherChildId, scheduleDetailId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);

			verify(sd, never()).changeScheduleStatus(any());
		}

		@Test
		void 일정상태가_pending이거나_verified가_아니면_예외() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 10L;

			ScheduleDetail sd = mock(ScheduleDetail.class);
			Schedule schedule = mock(Schedule.class);
			Child child = mock(Child.class);

			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(scheduleDetailRepository.findById(scheduleDetailId)).willReturn(Optional.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getChild()).willReturn(child);
			given(child.getId()).willReturn(childId);

			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);

			// when & then
			assertThatThrownBy(() -> scheduleService.skipNowSchedule(childId, scheduleDetailId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.SCHEDULE_COULD_NOT_BE_SKIPPED);
		}
	}

	@Nested
	@DisplayName("getSchedules")
	class GetSchedules {
		@Test
		void 부모가_없으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			given(parentRepository.findById(parentId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.getSchedules(null, null, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.PARENT_NOT_FOUND);
		}

		@Test
		void 아이가_없으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			Parent parent = mock(Parent.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> scheduleService.getSchedules(null, null, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ChildErrorCode.CHILD_NOT_FOUND);
		}

		@Test
		void 자신의_아이가_아니면_접근제한() {
			// given
			Long parentId = 1L;
			Long otherChildId = 100L;

			Parent parent = mock(Parent.class);
			Child otherChild = mock(Child.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(otherChildId)).willReturn(Optional.of(otherChild));
			given(parentChildRepository.existsByParentAndChild(parent, otherChild)).willReturn(false);

			// when & then
			assertThatThrownBy(() -> scheduleService.getSchedules(null, null, parentId, otherChildId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ParentErrorCode.NOT_ALLOWED_TO_CHILD);
		}

		@Test
		void startDate와_endDate의_순서가_유효하지_않으면_예외() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			LocalDate startDate = LocalDate.of(2026, 1, 1);
			LocalDate endDate = LocalDate.of(2000, 1, 31);

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			// when & then
			assertThatThrownBy(
				() -> scheduleService.getSchedules(startDate, endDate, parentId, childId))
				.isInstanceOf(KieroException.class)
				.extracting(e -> ((KieroException)e).getBaseCode())
				.isEqualTo(ScheduleErrorCode.INVALID_DATE_DURATION);
		}

		@Test
		void 정상이고_해당하는_일정이_없으면_빈_응답을_반환() {
			// given
			Long parentId = 1L;
			Long childId = 1L;

			LocalDate startDate = LocalDate.of(2026, 1, 1);
			LocalDate endDate = LocalDate.of(2026, 1, 31);

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);
			given(scheduleRepository.findAllByChildId(childId)).willReturn(List.of());

			// when
			ScheduleTabResponse response = scheduleService.getSchedules(startDate, endDate, parentId, childId);

			// then
			assertThat(response.recurringSchedules()).isEqualTo(List.of());
			assertThat(response.normalSchedules()).isEqualTo(List.of());
		}

		@Test
		void 정상이고_반복일정이_있으면_recurringScheduleDto_채우기() {
			// given
			Long parentId = 1L;
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			LocalDate startDate = LocalDate.of(2026, 1, 1);
			LocalDate endDate = LocalDate.of(2026, 1, 31);

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			Schedule schedule = mock(Schedule.class);
			Long scheduleId = 100L;

			ScheduleRepeatDays rdMon = mock(ScheduleRepeatDays.class);
			ScheduleRepeatDays rdWed = mock(ScheduleRepeatDays.class);

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			given(scheduleRepository.findAllByChildId(childId)).willReturn(List.of(schedule));

			given(schedule.getId()).willReturn(scheduleId);
			given(schedule.isRecurring()).willReturn(true);

			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 30));
			given(schedule.getName()).willReturn("첫번째 일정");
			given(schedule.getScheduleColor()).willReturn(ScheduleColor.SCHEDULE1);

			// 불피우기 여부
			given(scheduleDetailRepository.existsStoneUsedToday(eq(List.of(scheduleId)), any(LocalDate.class)))
				.willReturn(false);

			// repeatDays -> scheduleId 매핑되도록 세팅
			given(rdMon.getSchedule()).willReturn(schedule);
			given(rdWed.getSchedule()).willReturn(schedule);
			given(rdMon.getDayOfWeek()).willReturn(DayOfWeek.MON);
			given(rdWed.getDayOfWeek()).willReturn(DayOfWeek.WED);

			given(scheduleRepeatDaysRepository.findAllByScheduleIdsIn(eq(List.of(scheduleId))))
				.willReturn(List.of(rdMon, rdWed));

			// when
			ScheduleTabResponse response = scheduleService.getSchedules(startDate, endDate, parentId, childId);

			// then
			assertThat(response.isFireLit()).isFalse();
			assertThat(response.recurringSchedules()).hasSize(1);
			assertThat(response.normalSchedules()).isEmpty();

			RecurringScheduleDto dto = response.recurringSchedules().get(0);
			assertThat(dto.startTime()).isEqualTo(LocalTime.of(11, 0));
			assertThat(dto.endTime()).isEqualTo(LocalTime.of(11, 30));
			assertThat(dto.name()).isEqualTo("첫번째 일정");
			assertThat(dto.colorCode()).isEqualTo(ScheduleColor.SCHEDULE1.getColorCode());
			assertThat(dto.dayOfWeek()).isEqualTo("MON, WED");

			verify(scheduleDetailRepository, never())
				.findAllByScheduleIdInAndDateBetween(anyList(), any(LocalDate.class), any(LocalDate.class));
		}

		@Test
		void 정상이고_기간에_해당하는_단일일정이_있으면_normalScheduleDto_채우기() {
			// given
			Long parentId = 1L;
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			LocalDate startDate = LocalDate.of(2026, 1, 1);
			LocalDate endDate = LocalDate.of(2026, 1, 31);
			LocalDate scheduleDate = LocalDate.of(2026, 1, 17);

			Parent parent = mock(Parent.class);
			Child child = mock(Child.class);

			Schedule schedule = mock(Schedule.class);
			ScheduleDetail scheduleDetail = mock(ScheduleDetail.class);
			Long scheduleId = 100L;

			given(parentRepository.findById(parentId)).willReturn(Optional.of(parent));
			given(childRepository.findById(childId)).willReturn(Optional.of(child));
			given(parentChildRepository.existsByParentAndChild(parent, child)).willReturn(true);

			given(scheduleRepository.findAllByChildId(childId)).willReturn(List.of(schedule));

			given(schedule.getId()).willReturn(scheduleId);
			given(schedule.isRecurring()).willReturn(false);

			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 30));
			given(schedule.getName()).willReturn("첫번째 일정");
			given(schedule.getScheduleColor()).willReturn(ScheduleColor.SCHEDULE1);

			// 불피우기 여부
			given(scheduleDetailRepository.existsStoneUsedToday(eq(List.of(scheduleId)), any(LocalDate.class)))
				.willReturn(false);

			given(scheduleDetail.getSchedule()).willReturn(schedule);
			given(scheduleDetail.getDate()).willReturn(scheduleDate);

			given(scheduleDetailRepository.findAllByScheduleIdInAndDateBetween(List.of(scheduleId), startDate,
				endDate)).willReturn(List.of(scheduleDetail));

			// when
			ScheduleTabResponse response = scheduleService.getSchedules(startDate, endDate, parentId, childId);

			// then
			assertThat(response.isFireLit()).isFalse();
			assertThat(response.recurringSchedules()).isEmpty();
			assertThat(response.normalSchedules()).hasSize(1);

			NormalScheduleDto dto = response.normalSchedules().get(0);
			assertThat(dto.startTime()).isEqualTo(LocalTime.of(11, 0));
			assertThat(dto.endTime()).isEqualTo(LocalTime.of(11, 30));
			assertThat(dto.name()).isEqualTo("첫번째 일정");
			assertThat(dto.colorCode()).isEqualTo(ScheduleColor.SCHEDULE1.getColorCode());
			assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 1, 17));

			verify(scheduleRepeatDaysRepository, never())
				.findAllByScheduleIdsIn(anyList());
		}
	}

	@Nested
	@DisplayName("createTodayScheduleDetail")
	class CreateTodayScheduleDetail {
		@Test
		void 정상이면_오늘의_반복일정_detail_생성() {
			// given
			LocalDate fixedDate = LocalDate.of(2026, 1, 16); // FRI
			Clock clockFri = Clock.fixed(
				fixedDate.atTime(11, 30).atZone(KST).toInstant(),
				KST
			);
			ReflectionTestUtils.setField(scheduleService, "clock", clockFri);

			given(scheduleRepeatDaysRepository.findSchedulesToCreateTodayDetail(DayOfWeek.FRI, fixedDate))
				.willReturn(List.of(mock(Schedule.class)));

			// when
			scheduleService.createTodayScheduleDetail();

			// then
			ArgumentCaptor<List<ScheduleDetail>> captor = ArgumentCaptor.forClass(List.class);
			verify(scheduleDetailRepository).saveAll(captor.capture());
			assertThat(captor.getValue()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("getTodaySchedule")
	class GetTodaySchedule {

		@Test
		void 오늘_일정이_없으면_빈_응답_반환() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of());

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleDetailId()).isNull();
			assertThat(response.scheduleOrder()).isEqualTo(0);
			assertThat(response.totalSchedule()).isEqualTo(0);
			assertThat(response.earnedStones()).isEqualTo(0);
			assertThat(response.scheduleStatus()).isEqualTo(TodayScheduleStatus.NO_SCHEDULE);
			assertThat(response.isSkippable()).isFalse();
			assertThat(response.isNowScheduleVerified()).isFalse();
		}

		@Test
		void 모든_일정이_SKIPPED면_totalSchedule은_0() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);

			Schedule schedule = mock(Schedule.class);
			given(schedule.getCreatedAt()).willReturn(today.atStartOfDay());
			given(schedule.getStartTime()).willReturn(LocalTime.of(23, 59));
			given(sd1.getSchedule()).willReturn(schedule);
			given(sd2.getSchedule()).willReturn(schedule);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleDetailId()).isNull();
			assertThat(response.scheduleOrder()).isEqualTo(0);
			assertThat(response.totalSchedule()).isEqualTo(0);
			assertThat(response.earnedStones()).isEqualTo(0);
			assertThat(response.scheduleStatus()).isEqualTo(TodayScheduleStatus.FIRE_NOT_LIT);
			assertThat(response.isSkippable()).isFalse();
			assertThat(response.isNowScheduleVerified()).isFalse();
		}

		@Test
		void 오늘_반복일정이_있으면_scheduleDetail_자동_생성() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Schedule schedule = mock(Schedule.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of(schedule));
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of());

			// when
			scheduleService.getTodaySchedule(childId);

			// then
			ArgumentCaptor<List<ScheduleDetail>> captor = ArgumentCaptor.forClass(List.class);
			verify(scheduleDetailRepository).saveAll(captor.capture());

			List<ScheduleDetail> saved = captor.getValue();
			assertThat(saved).hasSize(1);

			ScheduleDetail created = saved.get(0);
			assertThat(created.getSchedule()).isSameAs(schedule);
			assertThat(created.getDate()).isEqualTo(today);
			assertThat(created.getScheduleStatus()).isEqualTo(ScheduleStatus.PENDING);
		}

		@Test
		void 오늘_반복일정이_이미_detail이_있으면_중복_생성되지_않음() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of());

			// when
			scheduleService.getTodaySchedule(childId);

			// then
			verify(scheduleDetailRepository, never()).saveAll(anyList());
		}

		@Test
		void 오늘_생성되지_않은_일정은_필터에서_제외되지_않음() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule schedule = mock(Schedule.class);
			given(schedule.getCreatedAt()).willReturn(today.minusDays(1).atStartOfDay());
			given(schedule.getStartTime()).willReturn(LocalTime.of(9, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(10, 0));
			given(schedule.getName()).willReturn("어제 생성된 일정");

			ScheduleDetail sd = mock(ScheduleDetail.class);
			given(sd.getSchedule()).willReturn(schedule);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd.getStoneUsedAt()).willReturn(null);

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.totalSchedule()).isEqualTo(1);
		}

		@Test
		void createdAt이_startTime_이후면_제외() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(23, 59)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(0, 0));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.totalSchedule()).isEqualTo(0);
		}

		@Test
		void 불피우기_이후에_생성된_일정은_제외() {
			// given
			Long childId = 1L;
			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);
			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.COMPLETED);
			given(schedule.getStartTime()).willReturn(LocalTime.of(23, 59));
			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(12, 0)));
			given(sd.getStoneUsedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.totalSchedule()).isEqualTo(0);
		}

		@Test
		void 종료시간이_지났고_PENDING이면_FAILED로_변경() {
			// given
			Long childId = 1L;

			Clock afterEndClock = Clock.fixed(
				today.atTime(12, 0).atZone(KST).toInstant(),
				KST
			);
			ReflectionTestUtils.setField(scheduleService, "clock", afterEndClock);
			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);

			// when
			scheduleService.getTodaySchedule(childId);

			// then
			verify(sd).changeScheduleStatus(ScheduleStatus.FAILED);
		}

		@Test
		void 종료시간이_지났고_VERIFIED면_COMPLETED로_변경() {
			// given
			Long childId = 1L;

			Clock afterEndClock = Clock.fixed(
				today.atTime(12, 0).atZone(KST).toInstant(),
				KST
			);
			ReflectionTestUtils.setField(scheduleService, "clock", afterEndClock);
			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getSchedule()).willReturn(schedule);
			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);

			// when
			scheduleService.getTodaySchedule(childId);

			// then
			verify(sd).changeScheduleStatus(ScheduleStatus.COMPLETED);
		}

		@Test
		void PENDING이_있으면_todoSchedule로_선택됨() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any())).willReturn(
				List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));

			given(sd.getSchedule()).willReturn(schedule);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(schedule.getName()).willReturn("테스트 일정");

			given(sd.getId()).willReturn(scheduleDetailId);
			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleDetailId()).isEqualTo(1L);
		}

		@Test
		void VERIFIED만_있으면_todoSchedule로_선택됨() {
			// given
			Long childId = 1L;
			Long scheduleDetailId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule schedule = mock(Schedule.class);
			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd));

			given(sd.getSchedule()).willReturn(schedule);
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);
			given(sd.getId()).willReturn(scheduleDetailId);

			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));
			given(schedule.getName()).willReturn("테스트 일정");

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleDetailId()).isEqualTo(scheduleDetailId);
			assertThat(response.isNowScheduleVerified()).isTrue();
		}

		@Test
		void todoSchedule이_없으면_null_반환() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of());

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleDetailId()).isNull();
		}

		@Test
		void nextTodoSchedule이_있으면_isSkippable_true() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1, sd2));
			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd2.getSchedule()).willReturn(s2);

			given(s1.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(s1.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(s1.getEndTime()).willReturn(LocalTime.of(11, 59));

			given(s2.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(s2.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(s2.getEndTime()).willReturn(LocalTime.of(11, 59));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.isSkippable()).isTrue();
		}

		@Test
		void nextTodoSchedule이_없으면_isSkippable_false() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1));
			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd1.getSchedule()).willReturn(s1);

			given(s1.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(s1.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(s1.getEndTime()).willReturn(LocalTime.of(11, 59));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.isSkippable()).isFalse();
		}

		@Test
		void todoSchedule_순서에_따라_StoneType_COURAGE() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd1));
			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd1.getSchedule()).willReturn(s1);

			given(s1.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(s1.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(s1.getEndTime()).willReturn(LocalTime.of(11, 59));

			final StoneType[] holder = new StoneType[1];
			doAnswer(inv -> { holder[0] = inv.getArgument(0); return null; })
				.when(sd1).changeStoneType(any(StoneType.class));
			given(sd1.getStoneType()).willAnswer(inv -> holder[0]);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.stoneType()).isEqualTo(StoneType.COURAGE);
		}

		@Test
		void todoSchedule_순서에_따라_StoneType_GRIT() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2));

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd1.getStoneUsedAt()).willReturn(null);

			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd2.getSchedule()).willReturn(s2);
			given(sd2.getStoneUsedAt()).willReturn(null);

			LocalDateTime createdAt = LocalDateTime.of(today, LocalTime.of(0, 0));
			LocalTime startTime = LocalTime.of(11, 0);
			LocalTime endTime = LocalTime.of(11, 59);

			given(s1.getCreatedAt()).willReturn(createdAt);
			given(s1.getStartTime()).willReturn(startTime);

			given(s2.getCreatedAt()).willReturn(createdAt);
			given(s2.getStartTime()).willReturn(startTime);
			given(s2.getEndTime()).willReturn(endTime);

			final StoneType[] holder = new StoneType[1];
			doAnswer(inv -> {
				holder[0] = inv.getArgument(0);
				return null;
			})
				.when(sd2).changeStoneType(any(StoneType.class));
			given(sd2.getStoneType()).willAnswer(inv -> holder[0]);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.stoneType()).isEqualTo(StoneType.GRIT);
			verify(sd2).changeStoneType(StoneType.GRIT);
		}

		@Test
		void todoSchedule_순서에_따라_StoneType_WISDOM() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);
			Schedule s3 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);
			ScheduleDetail sd3 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2, sd3));

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd1.getStoneUsedAt()).willReturn(null);

			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd2.getSchedule()).willReturn(s2);
			given(sd2.getStoneUsedAt()).willReturn(null);

			given(sd3.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd3.getSchedule()).willReturn(s3);
			given(sd3.getStoneUsedAt()).willReturn(null);

			LocalDateTime createdAt = LocalDateTime.of(today, LocalTime.of(0, 0));
			LocalTime startTime = LocalTime.of(11, 0);
			LocalTime endTime = LocalTime.of(11, 59);

			given(s1.getCreatedAt()).willReturn(createdAt);
			given(s1.getStartTime()).willReturn(startTime);

			given(s2.getCreatedAt()).willReturn(createdAt);
			given(s2.getStartTime()).willReturn(startTime);

			given(s3.getCreatedAt()).willReturn(createdAt);
			given(s3.getStartTime()).willReturn(startTime);
			given(s3.getEndTime()).willReturn(endTime);

			final StoneType[] holder = new StoneType[1];
			doAnswer(inv -> {
				holder[0] = inv.getArgument(0);
				return null;
			})
				.when(sd3).changeStoneType(any(StoneType.class));
			given(sd3.getStoneType()).willAnswer(inv -> holder[0]);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.stoneType()).isEqualTo(StoneType.WISDOM);
			verify(sd3).changeStoneType(StoneType.WISDOM);
		}

		@Test
		void SKIPPED_제외_todoSchedule_계산() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2));

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd1.getStoneUsedAt()).willReturn(null);

			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd2.getSchedule()).willReturn(s2);
			given(sd2.getStoneUsedAt()).willReturn(null);

			LocalDateTime createdAt = LocalDateTime.of(today, LocalTime.of(0, 0));
			LocalTime startTime = LocalTime.of(11, 0);
			LocalTime endTime = LocalTime.of(11, 59);

			given(s1.getCreatedAt()).willReturn(createdAt);
			given(s1.getStartTime()).willReturn(startTime);

			given(s2.getCreatedAt()).willReturn(createdAt);
			given(s2.getStartTime()).willReturn(startTime);
			given(s2.getEndTime()).willReturn(endTime);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.totalSchedule()).isEqualTo(1);
		}

		@Test
		void VERIFIED_COMPLETED만_earnedStones로_계산() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2));

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd1.getStoneUsedAt()).willReturn(null);

			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);
			given(sd2.getSchedule()).willReturn(s2);
			given(sd2.getStoneUsedAt()).willReturn(null);

			LocalDateTime createdAt = LocalDateTime.of(today, LocalTime.of(0, 0));
			LocalTime startTime = LocalTime.of(11, 0);
			LocalTime endTime = LocalTime.of(11, 59);

			given(s1.getCreatedAt()).willReturn(createdAt);
			given(s1.getStartTime()).willReturn(startTime);

			given(s2.getCreatedAt()).willReturn(createdAt);
			given(s2.getStartTime()).willReturn(startTime);
			given(s2.getEndTime()).willReturn(endTime);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.earnedStones()).isEqualTo(1);
		}

		@Test
		void scheduleOrder는_filteredAllScheduleDetails_기준() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule s1 = mock(Schedule.class);
			Schedule s2 = mock(Schedule.class);

			ScheduleDetail sd1 = mock(ScheduleDetail.class);
			ScheduleDetail sd2 = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());

			given(scheduleDetailRepository.findByDateAndChildId(today, childId))
				.willReturn(List.of(sd1, sd2));

			given(sd1.getScheduleStatus()).willReturn(ScheduleStatus.SKIPPED);
			given(sd1.getSchedule()).willReturn(s1);
			given(sd1.getStoneUsedAt()).willReturn(null);

			given(sd2.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);
			given(sd2.getSchedule()).willReturn(s2);
			given(sd2.getStoneUsedAt()).willReturn(null);

			LocalDateTime createdAt = LocalDateTime.of(today, LocalTime.of(0, 0));
			LocalTime startTime = LocalTime.of(11, 0);
			LocalTime endTime = LocalTime.of(11, 59);

			given(s1.getCreatedAt()).willReturn(createdAt);
			given(s1.getStartTime()).willReturn(startTime);

			given(s2.getCreatedAt()).willReturn(createdAt);
			given(s2.getStartTime()).willReturn(startTime);
			given(s2.getEndTime()).willReturn(endTime);

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.scheduleOrder()).isEqualTo(2);
		}

		@Test
		void todoSchedule이_VERIFIED면_isNowScheduleVerified_true() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule schedule = mock(Schedule.class);

			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.VERIFIED);
			given(sd.getSchedule()).willReturn(schedule);

			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.isNowScheduleVerified()).isTrue();
		}

		@Test
		void todoSchedule이_PENDING면_isNowScheduleVerified_false() {
			// given
			Long childId = 1L;

			ReflectionTestUtils.setField(scheduleService, "clock", fixedClock);

			Schedule schedule = mock(Schedule.class);

			ScheduleDetail sd = mock(ScheduleDetail.class);

			given(scheduleRepository.findRecurringSchedulesToGenerateTodayDetail(any(), any(), any()))
				.willReturn(List.of());
			given(scheduleDetailRepository.findByDateAndChildId(today, childId)).willReturn(List.of(sd));
			given(sd.getScheduleStatus()).willReturn(ScheduleStatus.PENDING);
			given(sd.getSchedule()).willReturn(schedule);

			given(schedule.getCreatedAt()).willReturn(LocalDateTime.of(today, LocalTime.of(0, 0)));
			given(schedule.getStartTime()).willReturn(LocalTime.of(11, 0));
			given(schedule.getEndTime()).willReturn(LocalTime.of(11, 59));

			// when
			TodayScheduleResponse response = scheduleService.getTodaySchedule(childId);

			// then
			assertThat(response.isNowScheduleVerified()).isFalse();
		}

	}
}
