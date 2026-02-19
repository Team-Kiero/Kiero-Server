package com.kiero.missions.application.port.in;

import java.util.List;

import com.kiero.missions.application.dto.MissionBulkCreateRequest;
import com.kiero.missions.application.dto.MissionCreateRequest;
import com.kiero.missions.application.dto.MissionResponse;

public interface MissionCommandUseCase {
	MissionResponse createMission(Long parentId, Long childId, MissionCreateRequest request);

	List<MissionResponse> bulkCreateMissions(Long parentId, Long childId, MissionBulkCreateRequest request);

	MissionResponse completeMission(Long childId, Long missionId);

}
