package com.kiero.schedule.application.port.out;

import java.util.List;

public interface ScheduleDeletePort {
	void deleteAllByChildId(Long childId);
	List<String> findImageKeysByChildId(Long childId);
}
