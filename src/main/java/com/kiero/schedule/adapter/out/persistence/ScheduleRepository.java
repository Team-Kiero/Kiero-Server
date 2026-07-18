package com.kiero.schedule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	List<Schedule> findAllByChildId(Long childId);

	List<Schedule> findAllByParentId(Long parentId);

	Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId);

	@Modifying
	@Query("""                                                                               
      DELETE FROM Schedule s
      WHERE s.isRecurring = false
      AND NOT EXISTS (
          SELECT 1 FROM ScheduleDetail sd WHERE sd.schedule = s
      )
  """)
	void deleteObsoleteNonRecurringSchedules();

	@Modifying
	@Query(value = "UPDATE schedule SET parent_id = :newParentId WHERE parent_id = :oldParentId AND child_id = :childId", nativeQuery = true)
	void transferOwnershipByParentIdAndChildId(@Param("oldParentId") Long oldParentId, @Param("newParentId") Long newParentId, @Param("childId") Long childId);
}
