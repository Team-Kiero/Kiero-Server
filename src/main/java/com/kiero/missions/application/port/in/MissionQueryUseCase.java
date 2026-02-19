package com.kiero.missions.application.port.in;

import java.util.List;

import com.kiero.missions.application.dto.MissionResponse;

public interface MissionQueryUseCase {
	List<MissionResponse> getMissionsByParent(Long parentId, Long childId);

	List<MissionResponse> getMissionsByChild(Long childId);
}
