package com.kiero.schedule.application.port.in;

import java.time.LocalDate;

import com.kiero.schedule.application.dto.FireLitResponse;
import com.kiero.schedule.application.dto.NowScheduleCompleteRequest;
import com.kiero.schedule.application.dto.ScheduleAddRequest;
import com.kiero.schedule.application.dto.ScheduleDeleteRequest;
import com.kiero.schedule.application.dto.ScheduleModifyRequest;

public interface ScheduleCommandUseCase {

	void addSchedule(ScheduleAddRequest request, Long parentId, Long childId);

	void skipNowSchedule(Long childId, Long scheduleDetailId);

	void completeNowSchedule(Long childId, Long scheduleDetailId, NowScheduleCompleteRequest request);

	FireLitResponse fireLit(Long childId);

	void updateSchedule(Long parentId, Long scheduleId, LocalDate selectedDate, ScheduleModifyRequest request);

	void deleteSchedule(Long parentId, Long scheduleId, LocalDate startDate, LocalDate endDate, LocalDate selectedDate, ScheduleDeleteRequest request);
}