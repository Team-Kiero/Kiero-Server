package com.kiero.schedule.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.DiscardedSchedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

@Repository
public interface DiscardedScheduleRepository extends JpaRepository<DiscardedSchedule, Long> {

	@Query("""
		select ds
		from DiscardedSchedule ds
		where ds.schedule.child.id = :childId
		  and ds.dayOfWeek in :dayOfWeeks
		""")
	List<DiscardedSchedule> findAllByChildIdAndDayOfWeekIn(
		@Param("childId") Long childId,
		@Param("dayOfWeeks") List<DayOfWeek> dayOfWeeks
	);

	@Query("""
		select ds
		from DiscardedSchedule ds
		where ds.schedule.child.id = :childId
		  and ds.date in :dates
		""")
	List<DiscardedSchedule> findAllByChildIdAndDateIn(
		@Param("childId") Long childId,
		@Param("dates") List<LocalDate> dates
	);

	@Query("""
		select ds
		from DiscardedSchedule ds
		where ds.schedule.child.id = :childId
		  and ds.date between :startDate and :endDate
		""")
	List<DiscardedSchedule> findAllByChildIdAndDateBetween(
		@Param("childId") Long childId,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);

	List<DiscardedSchedule> findAllByDate(LocalDate today);

	void deleteByScheduleId(Long id);
}
