package com.kiero.mission.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.mission.application.port.out.MissionPersistencePort;
import com.kiero.mission.domain.Mission;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionPersistenceAdapter implements MissionPersistencePort {

	private final MissionRepository missionRepository;

	@Override
	public Mission save(Mission mission) {
		return missionRepository.save(mission);
	}

	@Override
	public List<Mission> saveAll(List<Mission> missions) {
		return missionRepository.saveAll(missions);
	}

	@Override
	public List<Mission> findAllByChildIdAndDueAtGreaterThanEqual(Long childId, LocalDate date) {
		return missionRepository.findAllByChildIdAndDueAtGreaterThanEqual(childId, date);
	}

	@Override
	public List<Mission> findAllByParentIdAndDueAtGreaterThanEqual(Long parentId, LocalDate date) {
		return missionRepository.findAllByParentIdAndDueAtGreaterThanEqual(parentId, date);
	}

	@Override
	public Optional<Mission> findById(Long missionId) {
		return missionRepository.findById(missionId);
	}

	@Override
	public Optional<Mission> findByIdWithLock(Long missionId) {
		return missionRepository.findByIdWithLock(missionId);
	}

	@Override
	public void delete(Mission mission) {
		missionRepository.delete(mission);
	}

	@Override
	public List<Mission> findAllByChildIdAndDueAt(Long childId, LocalDate date) {
		return missionRepository.findAllByChildIdAndDueAt(childId, date);
	}

}
