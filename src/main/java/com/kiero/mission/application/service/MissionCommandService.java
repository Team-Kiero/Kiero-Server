package com.kiero.mission.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.exception.ChildErrorCode;
import com.kiero.child.application.port.in.ChildByIdUseCase;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.application.dto.MissionBulkCreateRequest;
import com.kiero.mission.application.dto.MissionCompleteEvent;
import com.kiero.mission.application.dto.MissionCreateRequest;
import com.kiero.mission.application.dto.MissionCreatedEvent;
import com.kiero.mission.application.dto.MissionResponse;
import com.kiero.mission.application.exception.MissionErrorCode;
import com.kiero.mission.application.port.in.MissionCommandUseCase;
import com.kiero.mission.application.port.out.MissionEventPort;
import com.kiero.mission.application.port.out.MissionPersistencePort;
import com.kiero.mission.domain.Mission;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.in.ParentByIdUseCase;
import com.kiero.parent.application.port.in.ParentChildAccessUseCase;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionCommandService implements MissionCommandUseCase {

	private final MissionEventPort eventPort;

	private final MissionPersistencePort missionPort;

	private final ParentByIdUseCase parentByIdUseCase;
	private final ParentChildAccessUseCase parentChildAccessUseCase;
	private final ChildByIdUseCase childByIdUseCase;

	@Override
	@Transactional
	public MissionResponse createMission(Long parentId, Long childId, MissionCreateRequest request) {
		validateParentChildRelation(parentId, childId);

		Parent parent = parentByIdUseCase.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		Child child = childByIdUseCase.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		Mission mission = Mission.create(
			parent,
			child,
			request.name(),
			request.reward(),
			request.dueAt()
		);

		Mission saved = missionPort.save(mission);

		eventPort.publish(new MissionCreatedEvent(
			child.getId(),
			mission.getName(),
			mission.getReward()
		));

		log.info("Mission created: missionId={}, parentId={}, childId={}, name={}",
			saved.getId(), parentId, childId, request.name());

		return MissionResponse.from(saved);
	}

	@Override
	@Transactional
	public List<MissionResponse> bulkCreateMissions(Long parentId, Long childId, MissionBulkCreateRequest request) {
		validateParentChildRelation(parentId, childId);

		Parent parent = parentByIdUseCase.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		Child child = childByIdUseCase.findById(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		List<Mission> missions = new ArrayList<>();
		for (MissionBulkCreateRequest.MissionItem item : request.missions()) {
			missions.add(Mission.create(parent, child, item.name(), item.reward(), item.dueAt()));
		}

		List<Mission> saved = missionPort.saveAll(missions);

		for (Mission m : saved) {
			eventPort.publish(new MissionCreatedEvent(childId, m.getName(), m.getReward()));
		}

		log.info("Bulk created {} missions for parentId={}, childId={}", saved.size(), parentId, childId);

		return saved.stream().map(MissionResponse::from).toList();
	}

	@Override
	@Transactional
	public MissionResponse completeMission(Long childId, Long missionId) {
		Mission mission = missionPort.findByIdWithLock(missionId)
			.orElseThrow(() -> new KieroException(MissionErrorCode.MISSION_NOT_FOUND));

		if (!mission.getChild().getId().equals(childId)) {
			throw new KieroException(MissionErrorCode.NOT_YOUR_MISSION);
		}

		if (mission.isCompleted()) {
			throw new KieroException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
		}

		if (mission.getDueAt().isBefore(LocalDate.now())) {
			throw new KieroException(MissionErrorCode.MISSION_EXPIRED);
		}

		Child child = childByIdUseCase.findByIdWithLock(childId)
			.orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

		mission.complete();
		child.addCoin(mission.getReward());

		eventPort.publish(new MissionCompleteEvent(
			child.getId(),
			mission.getReward(),
			mission.getName(),
			LocalDateTime.now()
		));

		log.info("Mission completed: missionId={}, childId={}, reward={}, newCoinAmount={}",
			missionId, childId, mission.getReward(), child.getCoinAmount());

		return MissionResponse.from(mission);
	}

	private void validateParentChildRelation(Long parentId, Long childId) {
		if (!parentChildAccessUseCase.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
		}
	}
}