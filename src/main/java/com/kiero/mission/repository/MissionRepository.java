package com.kiero.mission.repository;

import com.kiero.mission.domain.Mission;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("SELECT m FROM Mission m " +
           "WHERE m.child.id = :childId AND m.dueAt >= :date " +
           "ORDER BY m.dueAt ASC, m.createdAt DESC, m.name ASC")
    List<Mission> findAllByChildIdAndDueAtGreaterThanEqual(@Param("childId") Long childId, @Param("date") LocalDate date);

    @Query("SELECT m FROM Mission m " +
           "WHERE m.parent.id = :parentId AND m.dueAt >= :date " +
           "ORDER BY m.dueAt ASC, m.createdAt DESC, m.name ASC")
    List<Mission> findAllByParentIdAndDueAtGreaterThanEqual(@Param("parentId") Long parentId, @Param("date") LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"child"})
    @Query("SELECT m FROM Mission m WHERE m.id = :missionId")
    Optional<Mission> findByIdWithLock(@Param("missionId") Long missionId);
}
