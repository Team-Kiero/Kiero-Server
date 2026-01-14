package com.kiero.holiday.repository;

import com.kiero.holiday.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

}