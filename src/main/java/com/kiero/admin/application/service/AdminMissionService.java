package com.kiero.admin.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.dto.AdminMissionResponse;
import com.kiero.admin.application.dto.AdminMissionUpdateRequest;
import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminMissionUseCase;
import com.kiero.admin.application.port.out.AdminDeletePort;
import com.kiero.admin.application.port.out.AdminMissionLoadPort;
import com.kiero.global.exception.KieroException;
import com.kiero.mission.domain.Mission;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMissionService implements AdminMissionUseCase {

	private final AdminMissionLoadPort adminMissionLoadPort;
	private final AdminDeletePort adminDeletePort;

	@Override
	@Transactional(readOnly = true)
	public List<AdminMissionResponse> findAllByChildId(Long childId) {
		return adminMissionLoadPort.findAllByChildId(childId)
			.stream()
			.map(AdminMissionResponse::from)
			.toList();
	}

	@Override
	@Transactional
	public AdminMissionResponse updateMission(Long missionId, AdminMissionUpdateRequest request) {
		Mission mission = adminMissionLoadPort.findById(missionId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.MISSION_NOT_FOUND));

		mission.update(request.name(), request.reward(), request.dueAt());

		return AdminMissionResponse.from(mission);
	}

	@Override
	@Transactional
	public void deleteMission(Long missionId) {
		adminMissionLoadPort.findById(missionId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.MISSION_NOT_FOUND));

		adminDeletePort.deleteMissionById(missionId);
	}
}
