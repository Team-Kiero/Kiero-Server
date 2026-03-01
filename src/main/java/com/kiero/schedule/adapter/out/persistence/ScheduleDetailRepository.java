package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@Query("""
		select sd
		from ScheduleDetail sd
		join fetch sd.schedule s
		join fetch s.child c
		where sd.date = :date
		"""
	)
	List<ScheduleDetail> findAllByDate(
		@Param("date") LocalDate date
	);

	@Query("""
		select sd
		from ScheduleDetail sd
		join fetch sd.schedule s
		where sd.date in :dates
		  and s.child.id = :childId
		order by s.startTime asc
		""")
	List<ScheduleDetail> findByDateInAndChildId(
		@Param("dates") List<LocalDate> dates,
		@Param("childId") Long childId
	);

	@Query("""
		select distinct sd.schedule.child.id
		from ScheduleDetail sd
		where sd.date = :today
		  and (
			   sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.PENDING
			or sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.VERIFIED
		  )
		  and sd.schedule.endTime < :now
""")
	List<Long> findChildIdsToMark(
		@Param("today") LocalDate today,
		@Param("now") LocalTime now
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        update ScheduleDetail sd
           set sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.COMPLETED
         where sd.date = :today
           and sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.VERIFIED
           and sd.schedule.endTime < :now
    """)
	void bulkMarkVerifiedAsCompleted(
		@Param("today") LocalDate today,
		@Param("now") LocalTime now
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
        update ScheduleDetail sd
           set sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.FAILED
         where sd.date = :today
           and sd.scheduleStatus = com.kiero.schedule.domain.enums.ScheduleStatus.PENDING
           and sd.schedule.endTime < :now
    """)
	void bulkMarkPendingAsFailed(
		@Param("today") LocalDate today,
		@Param("now") LocalTime now
	);

	List<ScheduleDetail> findAllByScheduleChildIdAndDateGreaterThanEqual(Long childId, LocalDate date);

	@Modifying
	void deleteByScheduleIdAndDate(Long scheduleId, LocalDate date);

	Optional<ScheduleDetail> findByScheduleIdAndDate(Long scheduleId, LocalDate date);

	boolean existsByScheduleIdAndDate(Long scheduleId, LocalDate date);
}
