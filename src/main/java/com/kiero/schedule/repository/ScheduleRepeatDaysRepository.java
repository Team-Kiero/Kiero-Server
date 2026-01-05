package com.kiero.schedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.ScheduleRepeatDays;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface ScheduleRepeatDaysRepository extends JpaRepository<ScheduleRepeatDays, Long> {
	@Query("""
		select srd
		from ScheduleRepeatDays srd
		where srd.schedule.id
		in :scheduleIds
		""")
	List<ScheduleRepeatDays> findAllByScheduleIdsIn(
		@Param("scheduleIds") List<Long> scheduleIds
	);

}
