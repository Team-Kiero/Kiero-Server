package com.kiero.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kiero.schedule.domain.ScheduleRepeatDays;

@Repository
public interface ScheduleRepeatDaysRepository extends JpaRepository<ScheduleRepeatDays, Long> {
}
