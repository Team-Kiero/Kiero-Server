package com.kiero.mission.service;

import com.kiero.child.domain.Child;
import com.kiero.child.repository.ChildRepository;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.domain.Mission;
import com.kiero.mission.exception.MissionErrorCode;
import com.kiero.mission.presentation.dto.MissionBulkCreateRequest;
import com.kiero.mission.presentation.dto.MissionCompleteEvent;
import com.kiero.mission.presentation.dto.MissionCreateRequest;
import com.kiero.mission.presentation.dto.MissionResponse;
import com.kiero.mission.repository.MissionRepository;
import com.kiero.parent.exception.ParentErrorCode;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.*;

@ExtendWith(SpringExtension.class)
public class MissionServiceTest {

	@Mock
	MissionRepository missionRepository;
	@Mock
	ParentRepository parentRepository;
	@Mock
	ChildRepository childRepository;
	@Mock
	ParentChildRepository parentChildRepository;
	@Mock
	ApplicationEventPublisher eventPublisher;

	@InjectMocks
	MissionService missionService;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private Parent parent;
	private Child child;
	private Mission mission;

	@BeforeEach
	void setUp() {
		// Given: 기본 테스트 데이터 준비
		parent = Parent.builder()
			.id(1L)
			.email("parent@test.com")
			.name("테스트부모")
			.build();

		child = Child.builder()
			.id(1L)
			.lastName("홍")
			.firstName("길동")
			.coinAmount(100)
			.build();

		mission = Mission.builder()
			.id(1L)
			.name("수학 숙제하기")
			.reward(50)
			.dueAt(LocalDate.now().plusDays(1))
			.isCompleted(false)
			.parent(parent)
			.child(child)
			.build();
	}

	@Nested
	@DisplayName("completeMission")
	class CompleteMissionTest {

		@Test
		void 자녀가_미션을_완료하면_코인을_지급하고_이벤트를_발행한다() {
			// Given
			Long childId = 1L;
			Long missionId = 1L;
			int initialCoin = child.getCoinAmount();
			int reward = mission.getReward();

			given(missionRepository.findByIdWithLock(missionId))
				.willReturn(Optional.of(mission));
			given(childRepository.findByIdWithLock(childId))
				.willReturn(Optional.of(child));

			// When
			MissionResponse response = missionService.completeMission(childId, missionId);

			// Then 1: 미션 완료 상태 확인
			assertThat(mission.isCompleted()).isTrue();

			// Then 2: 코인 지급 확인
			assertThat(child.getCoinAmount()).isEqualTo(initialCoin + reward);

			// Then 3: 응답 검증
			assertThat(response).isNotNull();
			assertThat(response.id()).isEqualTo(missionId);
			assertThat(response.isCompleted()).isTrue();

			// Then 4: 이벤트 발행 확인
			ArgumentCaptor<MissionCompleteEvent> eventCaptor =
				ArgumentCaptor.forClass(MissionCompleteEvent.class);
			verify(eventPublisher).publishEvent(eventCaptor.capture());

			MissionCompleteEvent publishedEvent = eventCaptor.getValue();
			assertThat(publishedEvent.childId()).isEqualTo(childId);
			assertThat(publishedEvent.amount()).isEqualTo(reward);
			assertThat(publishedEvent.name()).isEqualTo("수학 숙제하기");

			// Then 5: Repository 호출 검증
			verify(missionRepository, times(1)).findByIdWithLock(missionId);
			verify(childRepository, times(1)).findByIdWithLock(childId);
		}

		@Test
		void 존재하지_않는_미션ID로_요청하면_예외가_발생한다() {
			// Given
			Long childId = 1L;
			Long invalidMissionId = 999L;

			given(missionRepository.findByIdWithLock(invalidMissionId))
				.willReturn(Optional.empty());

			// When & Then 1
			assertThatThrownBy(() -> missionService.completeMission(childId, invalidMissionId))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.MISSION_NOT_FOUND);

			// Then 2: 이벤트가 발행되지 않아야 함
			verify(eventPublisher, never()).publishEvent(any());
		}

		@Test
		void 다른_자녀의_미션을_완료하려고_하면_NOT_YOUR_MISSION_예외가_발생한다() {
			// Given
			Long otherChildId = 2L;
			Long missionId = 1L;

			given(missionRepository.findByIdWithLock(missionId))
				.willReturn(Optional.of(mission));

			// When & Then 1
			assertThatThrownBy(() -> missionService.completeMission(otherChildId, missionId))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.NOT_YOUR_MISSION);

			// Then 2: 미션이 완료되지 않아야 함
			assertThat(mission.isCompleted()).isFalse();

			// Then 3: 이벤트가 발행되지 않아야 함
			verify(eventPublisher, never()).publishEvent(any());
		}

		@Test
		void 완료된_미션을_완료하려고_하면_MISSION_ALREADY_COMPLETED_예외가_발생한다() {
			// Given
			mission.complete();
			Long childId = 1L;
			Long missionId = 1L;

			given(missionRepository.findByIdWithLock(missionId))
				.willReturn(Optional.of(mission));

			// When & Then 1
			assertThatThrownBy(() -> missionService.completeMission(childId, missionId))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.MISSION_ALREADY_COMPLETED);

			// Then 2: 이벤트가 발행되지 않아야 함
			verify(eventPublisher, never()).publishEvent(any());
		}

		@Test
		void 마감일이_지난_미션을_완료하려고_하면_MISSION_EXPIRED_예외가_발생한다() {
			// Given
			Mission expiredMission = Mission.builder()
				.id(1L)
				.name("지난 미션")
				.reward(50)
				.dueAt(LocalDate.of(2020, 1, 1))
				.isCompleted(false)
				.parent(parent)
				.child(child)
				.build();

			Long childId = 1L;
			Long missionId = 1L;

			given(missionRepository.findByIdWithLock(missionId))
				.willReturn(Optional.of(expiredMission));

			// When & Then 1
			assertThatThrownBy(() -> missionService.completeMission(childId, missionId))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.MISSION_EXPIRED);

			// Then 2: 미션이 완료되지 않아야 함
			assertThat(expiredMission.isCompleted()).isFalse();

			// Then 3: 코인이 지급되지 않아야 함
			assertThat(child.getCoinAmount()).isEqualTo(100);

			// Then 4: 이벤트가 발행되지 않아야 함
			verify(eventPublisher, never()).publishEvent(any());
		}
	}

	@Nested
	@DisplayName("createMission")
	class CreateMissionTest {

		@Test
		void 부모가_자녀에게_미션을_생성할_수_있다() {
			// Given
			Long parentId = 1L;
			Long childId = 1L;
			MissionCreateRequest request = new MissionCreateRequest(
				"영어 단어 외우기",
				30,
				LocalDate.of(2026, 1, 25)
			);

			Mission savedMission = Mission.builder()
				.id(100L)
				.name(request.name())
				.reward(request.reward())
				.dueAt(request.dueAt())
				.isCompleted(false)
				.parent(parent)
				.child(child)
				.build();

			given(parentChildRepository.existsByParentIdAndChildId(parentId, childId))
				.willReturn(true);
			given(parentRepository.findById(parentId))
				.willReturn(Optional.of(parent));
			given(childRepository.findById(childId))
				.willReturn(Optional.of(child));
			given(missionRepository.save(any(Mission.class)))
				.willReturn(savedMission);

			// When
			MissionResponse response = missionService.createMission(parentId, childId, request);

			// Then 1: 응답 검증
			assertThat(response).isNotNull();
			assertThat(response.id()).isEqualTo(100L);
			assertThat(response.name()).isEqualTo("영어 단어 외우기");
			assertThat(response.reward()).isEqualTo(30);
			assertThat(response.dueAt()).isEqualTo(LocalDate.of(2026, 1, 25));
			assertThat(response.isCompleted()).isFalse();

			// Then 2: 호출 검증
			verify(parentChildRepository).existsByParentIdAndChildId(parentId, childId);
			verify(parentRepository).findById(parentId);
			verify(childRepository).findById(childId);
			verify(missionRepository).save(any(Mission.class));
		}

		@Test
		void 부모_자녀_관계가_없으면_NOT_YOUR_CHILD_예외가_발생한다() {
			// Given
			Long parentId = 1L;
			Long otherChildId = 999L;
			MissionCreateRequest request = new MissionCreateRequest(
				"미션", 20, LocalDate.of(2026, 1, 25)
			);

			given(parentChildRepository.existsByParentIdAndChildId(parentId, otherChildId))
				.willReturn(false);

			// When & Then 1
			assertThatThrownBy(() -> missionService.createMission(parentId, otherChildId, request))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.NOT_YOUR_CHILD);

			// Then 2: save가 호출되지 않아야 함
			verify(missionRepository, never()).save(any());
		}

		@Test
		void 부모가_존재하지_않으면_PARENT_NOT_FOUND_예외가_발생한다() {
			// Given
			Long invalidParentId = 999L;
			Long childId = 1L;
			MissionCreateRequest request = new MissionCreateRequest(
				"미션", 20, LocalDate.of(2026, 1, 25)
			);

			given(parentChildRepository.existsByParentIdAndChildId(invalidParentId, childId))
				.willReturn(true);
			given(parentRepository.findById(invalidParentId))
				.willReturn(Optional.empty());

			// When & Then 1
			assertThatThrownBy(() -> missionService.createMission(invalidParentId, childId, request))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", ParentErrorCode.PARENT_NOT_FOUND);

			// Then 2: save가 호출되지 않아야 함
			verify(missionRepository, never()).save(any());
		}

		@Test
		void 자녀가_존재하지_않으면_CHILD_NOT_FOUND_예외가_발생한다() {
			// Given
			Long parentId = 1L;
			Long invalidChildId = 999L;
			MissionCreateRequest request = new MissionCreateRequest(
				"미션", 20, LocalDate.of(2026, 1, 25)
			);

			given(parentChildRepository.existsByParentIdAndChildId(parentId, invalidChildId))
				.willReturn(true);
			given(parentRepository.findById(parentId))
				.willReturn(Optional.of(parent));
			given(childRepository.findById(invalidChildId))
				.willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> missionService.createMission(parentId, invalidChildId, request))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", ChildErrorCode.CHILD_NOT_FOUND);

			verify(missionRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("bulkCreateMissions")
	class BulkCreateMissionsTest {

		@Test
		void 여러_미션을_일괄_생성할_수_있다() {
			// Given
			Long parentId = 1L;
			Long childId = 1L;

			List<MissionBulkCreateRequest.MissionItem> items = List.of(
				new MissionBulkCreateRequest.MissionItem("수학 숙제", 20, LocalDate.of(2026, 1, 20)),
				new MissionBulkCreateRequest.MissionItem("영어 단어", 30, LocalDate.of(2026, 1, 21)),
				new MissionBulkCreateRequest.MissionItem("일기 쓰기", 25, LocalDate.of(2026, 1, 22))
			);
			MissionBulkCreateRequest request = new MissionBulkCreateRequest(items);

			List<Mission> savedMissions = List.of(
				Mission.builder().id(1L).name("수학 숙제").reward(20).dueAt(LocalDate.of(2026, 1, 20))
					.isCompleted(false).parent(parent).child(child).build(),
				Mission.builder().id(2L).name("영어 단어").reward(30).dueAt(LocalDate.of(2026, 1, 21))
					.isCompleted(false).parent(parent).child(child).build(),
				Mission.builder().id(3L).name("일기 쓰기").reward(25).dueAt(LocalDate.of(2026, 1, 22))
					.isCompleted(false).parent(parent).child(child).build()
			);

			given(parentChildRepository.existsByParentIdAndChildId(parentId, childId))
				.willReturn(true);
			given(parentRepository.findById(parentId))
				.willReturn(Optional.of(parent));
			given(childRepository.findById(childId))
				.willReturn(Optional.of(child));
			given(missionRepository.saveAll(anyList()))
				.willReturn(savedMissions);

			// When
			List<MissionResponse> responses = missionService.bulkCreateMissions(parentId, childId, request);

			// Then 1: 응답 개수 검증
			assertThat(responses).hasSize(3);

			// Then 2: 각 응답 내용 검증
			assertThat(responses.get(0).name()).isEqualTo("수학 숙제");
			assertThat(responses.get(1).name()).isEqualTo("영어 단어");
			assertThat(responses.get(2).name()).isEqualTo("일기 쓰기");

			// Then 3: 호출 검증
			verify(missionRepository).saveAll(anyList());
		}

		@Test
		void 빈_리스트로_요청하면_빈_결과를_반환한다() {
			// Given
			Long parentId = 1L;
			Long childId = 1L;
			MissionBulkCreateRequest request = new MissionBulkCreateRequest(List.of());

			given(parentChildRepository.existsByParentIdAndChildId(parentId, childId))
				.willReturn(true);
			given(parentRepository.findById(parentId))
				.willReturn(Optional.of(parent));
			given(childRepository.findById(childId))
				.willReturn(Optional.of(child));
			given(missionRepository.saveAll(anyList()))
				.willReturn(List.of());

			// When
			List<MissionResponse> responses = missionService.bulkCreateMissions(parentId, childId, request);

			// Then
			assertThat(responses).isEmpty();
		}

		@Test
		void 부모_자녀_관계가_없으면_NOT_YOUR_CHILD_예외가_발생한다() {
			// Given
			Long parentId = 1L;
			Long otherChildId = 999L;

			List<MissionBulkCreateRequest.MissionItem> items = List.of(
				new MissionBulkCreateRequest.MissionItem("미션1", 20, LocalDate.of(2026, 1, 20))
			);
			MissionBulkCreateRequest request = new MissionBulkCreateRequest(items);

			given(parentChildRepository.existsByParentIdAndChildId(parentId, otherChildId))
				.willReturn(false);

			// When & Then 1
			assertThatThrownBy(() -> missionService.bulkCreateMissions(parentId, otherChildId, request))
				.isInstanceOf(KieroException.class)
				.hasFieldOrPropertyWithValue("baseCode", MissionErrorCode.NOT_YOUR_CHILD);

			// Then 2: saveAll이 호출되지 않아야 함
			verify(missionRepository, never()).saveAll(anyList());
		}
	}
}
