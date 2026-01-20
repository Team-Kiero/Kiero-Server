package com.kiero.schedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.ScheduleRepeatDays;
import com.kiero.schedule.domain.enums.DayOfWeek;

@Repository
public interface ScheduleRepeatDaysRepository extends JpaRepository<ScheduleRepeatDays, Long> {
	@Query("""
		select srd
		from ScheduleRepeatDays srd
		join fetch srd.schedule s
		where s.id in :scheduleIds
		""")
	List<ScheduleRepeatDays> findAllByScheduleIdsIn(
		@Param("scheduleIds") List<Long> scheduleIds
	);


	@Query("""
        select distinct srd.schedule
        from ScheduleRepeatDays srd
        where srd.dayOfWeek = :dayOfWeek
          and not exists (
              select 1
              from ScheduleDetail sd
              where sd.schedule = srd.schedule
                and sd.date = :date
          )
    """)
	List<Schedule> findSchedulesToCreateTodayDetail(
		@Param("dayOfWeek") DayOfWeek dayOfWeek,
		@Param("date") LocalDate date
	);

	@Query("""
		select s
		from ScheduleRepeatDays srd
		join srd.schedule s
		where srd.dayOfWeek in :dayOfWeeks
			and s.child.id = :childId
		""")
	List<Schedule> findSchedulesByChildIdAndDayOfWeeks(
		@Param("childId") Long childId,
		@Param("dayOfWeeks") List<DayOfWeek> dayOfWeeks
	);

	@Query("""
		select distinct s
		from ScheduleRepeatDays srd
		join srd.schedule s
		where srd.dayOfWeek in :dayOfWeeks
		  and s.child.id = :childId
		""")
	List<Schedule> findSchedulesByChildIdAndDayOfWeekIn(
		@Param("childId") Long childId,
		@Param("dayOfWeeks") List<DayOfWeek> dayOfWeeks
	);
}
