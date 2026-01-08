package com.kiero.schedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.ScheduleDetail;

@Repository
public interface ScheduleDetailRepository extends JpaRepository<ScheduleDetail, Long> {
	@Query("""
		 select sd
		 from ScheduleDetail sd
		 join fetch sd.schedule s
		 where s.id in :scheduleIds
		and sd.date between :startDate and :endDate
		""")
	List<ScheduleDetail> findAllByScheduleIdInAndDateBetween(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	@Query("""
				select (count(sd) > 0)
				from ScheduleDetail sd
				where sd.schedule.id in :scheduleIds
				  and sd.date = :date
				  and sd.stoneUsedAt is not null
		""")
	boolean existsStoneUsedToday(
		@Param("scheduleIds") List<Long> scheduleIds,
		@Param("date") LocalDate date
	);

	@Query("""
		select sd
		from ScheduleDetail sd
		join fetch sd.schedule s
		where sd.date = :date
		  and s.child.id = :childId
		order by s.startTime asc
		"""
	)
	List<ScheduleDetail> findByDateAndChildId(
		@Param("date") LocalDate date,
		@Param("childId") Long childId
	);
}
