package com.kiero.mission.application.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.exception.KieroException;
import com.kiero.mission.application.dto.MissionResponse;
import com.kiero.mission.application.exception.MissionErrorCode;
import com.kiero.mission.application.port.in.MissionQueryUseCase;
import com.kiero.mission.application.port.out.MissionPersistencePort;
import com.kiero.parent.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionQueryService implements MissionQueryUseCase {

	private final MissionPersistencePort missionPort;
	private final ParentChildAccessPort parentChildAccessPort;

	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> getMissionsByParent(Long parentId, Long childId) {
		LocalDate today = LocalDate.now();

		// 1. 특정 자녀 조회
		if (childId != null) {
			if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
				throw new KieroException(MissionErrorCode.NOT_YOUR_CHILD);
			}
			return missionPort.findAllByChildIdAndDueAtGreaterThanEqual(childId, today)
				.stream().map(MissionResponse::from).toList();
		}

		// 2. 전체 자녀 조회
		return missionPort.findAllByParentIdAndDueAtGreaterThanEqual(parentId, today)
			.stream().map(MissionResponse::from).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<MissionResponse> getMissionsByChild(Long childId) {
		LocalDate today = LocalDate.now();
		return missionPort.findAllByChildIdAndDueAtGreaterThanEqual(childId, today)
			.stream().map(MissionResponse::from).toList();
	}
}