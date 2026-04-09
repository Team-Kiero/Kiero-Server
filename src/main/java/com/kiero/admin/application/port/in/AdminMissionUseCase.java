package com.kiero.admin.application.port.in;

import java.util.List;

import com.kiero.admin.application.dto.AdminMissionResponse;
import com.kiero.admin.application.dto.AdminMissionUpdateRequest;

public interface AdminMissionUseCase {
	List<AdminMissionResponse> findAllByChildId(Long childId);
	AdminMissionResponse updateMission(Long missionId, AdminMissionUpdateRequest request);
	void deleteMission(Long missionId);
}
