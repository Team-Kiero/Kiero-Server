package com.kiero.schedule.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.Schedule;
import com.kiero.schedule.domain.enums.DayOfWeek;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
	List<Schedule> findAllByChildId(Long childId);

	@Query("""
		select distinct s
		from Schedule s
		join ScheduleRepeatDays rd on rd.schedule = s
		where s.isRecurring = true
		  and s.createdAt >= :startOfToday
		  and rd.dayOfWeek = :todayDayOfWeek
		  and not exists (
			  select 1
			  from ScheduleDetail sd
			  where sd.schedule = s
				and sd.date = :today
		      )
		""")
	List<Schedule> findRecurringSchedulesToGenerateTodayDetail(
		@Param("startOfToday") LocalDateTime startOfToday,
		@Param("todayDayOfWeek") DayOfWeek todayDayOfWeek,
		@Param("today") LocalDate today
	);

	Optional<Schedule> findFirstByChildIdOrderByCreatedAtDesc(Long childId);

	/*
	데모데이용 임시 메서드
	 */
	List<Schedule> findAllByChildIdIn(List<Long> childIds);
	/*
	 */
}
