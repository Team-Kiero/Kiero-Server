package com.kiero.mission.application.port.in;

import java.util.List;

import com.kiero.mission.application.dto.MissionProgressForParentDto;
import com.kiero.mission.application.dto.MissionResponse;

public interface MissionQueryUseCase {
	List<MissionResponse> getMissionsByParent(Long parentId, Long childId);

	List<MissionResponse> getMissionsByChild(Long childId);

	MissionProgressForParentDto getMissionTodayProgressForParent(Long childId);
}
