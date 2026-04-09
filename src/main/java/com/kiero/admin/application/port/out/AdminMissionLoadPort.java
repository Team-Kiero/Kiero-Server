package com.kiero.admin.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.mission.domain.Mission;

public interface AdminMissionLoadPort {
	List<Mission> findAllByChildId(Long childId);
	Optional<Mission> findById(Long missionId);
}
