package com.kiero.schedule.application.port.out;

public interface ScheduleDeletePort {
	void deleteAllByChildId(Long childId);
}
