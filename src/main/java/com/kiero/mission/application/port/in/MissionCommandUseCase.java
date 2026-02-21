package com.kiero.mission.application.port.in;

import java.util.List;

import com.kiero.mission.application.dto.MissionBulkCreateRequest;
import com.kiero.mission.application.dto.MissionCreateRequest;
import com.kiero.mission.application.dto.MissionResponse;
import com.kiero.mission.application.dto.MissionUpdateRequest;

public interface MissionCommandUseCase {
	MissionResponse createMission(Long parentId, Long childId, MissionCreateRequest request);

	List<MissionResponse> bulkCreateMissions(Long parentId, Long childId, MissionBulkCreateRequest request);

	MissionResponse completeMission(Long childId, Long missionId);

	MissionResponse updateMission(Long parentId, Long missionId, MissionUpdateRequest request);

	void deleteMission(Long parentId, Long missionId);

}
