package com.kiero.schedule.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	List<Schedule> findAllByChildId(Long childId);

	Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId);

}
