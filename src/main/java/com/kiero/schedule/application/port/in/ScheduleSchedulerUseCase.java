package com.kiero.schedule.application.port.in;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ScheduleSchedulerUseCase {

	void createTodayScheduleDetail();

	void bulkMarkAndPushEventIfUpdateExists(LocalDate today, LocalTime now);

	void deleteObsoleteNonRecurringSchedules();
}
