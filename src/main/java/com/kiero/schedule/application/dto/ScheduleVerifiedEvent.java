package com.kiero.schedule.application.dto;

import java.util.List;

public record ScheduleVerifiedEvent(
	List<Long> parentIds,
	Long childId,
	Long scheduleDetailId,
	String scheduleName
) {
}
