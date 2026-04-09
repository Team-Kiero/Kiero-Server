package com.kiero.admin.application.port.in;

import java.util.List;

import com.kiero.admin.application.dto.AdminScheduleResponse;

public interface AdminScheduleUseCase {
	List<AdminScheduleResponse> findAllByChildId(Long childId);
	void deleteSchedule(Long scheduleId);
}
