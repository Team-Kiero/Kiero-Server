package com.kiero.holidays.adapter.out.persistence;

import com.kiero.holidays.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);
}