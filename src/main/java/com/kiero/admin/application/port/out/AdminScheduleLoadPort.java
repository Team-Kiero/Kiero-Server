package com.kiero.admin.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.schedule.domain.Schedule;

public interface AdminScheduleLoadPort {
	List<Schedule> findAllByChildId(Long childId);
	Optional<Schedule> findById(Long scheduleId);
}
