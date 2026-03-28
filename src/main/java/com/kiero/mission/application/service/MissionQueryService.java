package com.kiero.mission.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.exception.KieroException;
import com.kiero.mission.application.dto.MissionProgressForParentDto;
import com.kiero.mission.application.dto.MissionResponse;
import com.kiero.mission.application.exception.MissionErrorCode;
import com.kiero.mission.application.port.in.MissionQueryUseCase;
import com.kiero.mission.application.port.out.MissionPersistencePort;
import com.kiero.mission.domain.Mission;
import com.kiero.parent.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionQueryService implements MissionQueryUseCase {

	private final MissionPersistencePort missionPort;
	private final MissionCacheableService missionCacheableService;
	private final ParentChildAccessPort parentChildAccessPort;

	@Override
	public List<MissionResponse> getMissionsByParent(Long parentId, Long childId) {
		// 1. 특정 자녀 조회
		if (childId != null) {
			if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
				throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
			}
			return missionCacheableService.getMissionsByChild(childId);
		}

		// 2. 전체 자녀 조회 (캐시 미적용)
		LocalDate today = LocalDate.now();
		return missionPort.findAllByParentIdAndDueAtGreaterThanEqual(parentId, today)
			.stream().map(MissionResponse::from).toList();
	}

	@Override
	public List<MissionResponse> getMissionsByChild(Long childId) {
		return missionCacheableService.getMissionsByChild(childId);
	}

	@Override
	@Transactional(readOnly = true)
	public MissionProgressForParentDto getMissionTodayProgressForParent(Long childId) {
		LocalDate today = LocalDate.now();

		List<Mission> allMissions = missionPort.findAllByChildIdAndDueAt(childId, today);

		List<MissionProgressForParentDto.MissionDto> completeMissions = allMissions.stream()
			.filter(Mission::isCompleted)
			.map(m -> new MissionProgressForParentDto.MissionDto(m.getName(), m.getReward()))
			.toList();

		List<MissionProgressForParentDto.MissionDto> incompleteMissions = allMissions.stream()
			.filter(m -> !m.isCompleted())
			.map(m -> new MissionProgressForParentDto.MissionDto(m.getName(), m.getReward()))
			.toList();

		return new MissionProgressForParentDto(completeMissions, incompleteMissions);
	}
}