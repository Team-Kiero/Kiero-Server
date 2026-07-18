package com.kiero.mission.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.kiero.mission.domain.Mission;

public interface MissionPersistencePort {
	Mission save(Mission mission);

	List<Mission> saveAll(List<Mission> missions);

	List<Mission> findAllByChildIdAndDueAtGreaterThanEqual(Long childId, LocalDate date);

	List<Mission> findAllByParentIdAndDueAtGreaterThanEqual(Long parentId, LocalDate date);

	Optional<Mission> findById(Long missionId);

	Optional<Mission> findByIdWithLockForChild(Long missionId);

	Optional<Mission> findByIdWithLockForParent(Long missionId);

	void delete(Mission mission);

	List<Mission> findAllByChildIdAndDueAt(Long childId, LocalDate date);

	List<Long> findDistinctChildIdsByDate(LocalDate today);

	List<Long> findChildIdsWithIncompleteMissionsByDate(LocalDate today);

}
