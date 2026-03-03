package com.kiero.schedule.application.port.in;

import java.time.LocalDate;

import com.kiero.schedule.application.dto.ScheduleProgressForChildResponse;
import com.kiero.schedule.application.dto.DefaultScheduleContentResponse;
import com.kiero.schedule.application.dto.ScheduleOccurrencesResponse;
import com.kiero.schedule.application.dto.ScheduleProgressForParentDto;
import com.kiero.schedule.application.dto.TodayScheduleResponse;

public interface ScheduleQueryUseCase {

	TodayScheduleResponse getTodaySchedule(Long childId);

	ScheduleOccurrencesResponse getSchedules(LocalDate startDate, LocalDate endDate, Long parentId, Long childId);

	DefaultScheduleContentResponse getDefaultSchedule(Long parentId, Long childId);

	ScheduleProgressForChildResponse getScheduleTodayProgressForChild(Long childId);

	ScheduleProgressForParentDto getScheduleTodayProgressForParent(Long parentId, Long childId);
}
