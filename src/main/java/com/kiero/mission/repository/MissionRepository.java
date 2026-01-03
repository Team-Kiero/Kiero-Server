package com.kiero.mission.repository;

import com.kiero.mission.domain.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findAllByChildId(Long childId);

    List<Mission> findAllByParentId(Long parentId);
}
