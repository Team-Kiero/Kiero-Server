package com.kiero.admin.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminMissionLoadPort;
import com.kiero.mission.adapter.out.persistence.MissionRepository;
import com.kiero.mission.domain.Mission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminMissionPersistenceAdapter implements AdminMissionLoadPort {

	private final MissionRepository missionRepository;

	@Override
	public List<Mission> findAllByChildId(Long childId) {
		return missionRepository.findAllByChildId(childId);
	}

	@Override
	public Optional<Mission> findById(Long missionId) {
		return missionRepository.findById(missionId);
	}
}
