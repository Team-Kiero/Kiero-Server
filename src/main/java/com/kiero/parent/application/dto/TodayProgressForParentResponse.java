package com.kiero.parent.application.dto;

import java.util.List;

import com.kiero.mission.application.dto.MissionProgressForParentDto;
import com.kiero.schedule.application.dto.ScheduleProgressForParentDto;

public record TodayProgressForParentResponse(
	boolean isFireLitToday,
	List<MissionProgressForParentDto.MissionDto> completeMissions,
	List<MissionProgressForParentDto.MissionDto> incompleteMissions,
	List<ScheduleProgressForParentDto.ScheduleDto> schedules
) {
}
