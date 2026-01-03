package com.kiero.mission.repository;

import com.kiero.mission.domain.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findAllByChildIdAndDueAtGreaterThanEqual(Long childId, LocalDate date);

    List<Mission> findAllByParentIdAndDueAtGreaterThanEqual(Long parentId, LocalDate date);
}
