package com.kiero.holiday.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.holiday.domain.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);
}