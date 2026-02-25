package com.kiero.schedule.application.port.in;

import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.TodayScheduleResponse;

public interface ScheduleCommandUseCase {

	void addSchedule(ScheduleAddRequest request, Long parentId, Long childId);

	TodayScheduleResponse getTodaySchedule(Long childId);

	void skipNowSchedule(Long childId, Long scheduleDetailId);

	void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request);

	FireLitResponse fireLit(Long childId);

	void createTodayScheduleDetail();
}