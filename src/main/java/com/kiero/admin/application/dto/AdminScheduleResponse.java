package com.kiero.admin.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.kiero.schedule.domain.Schedule;

public record AdminScheduleResponse(
	Long id,
	String name,
	LocalTime startTime,
	LocalTime endTime,
	String scheduleColor,
	boolean isRecurring,
	LocalDate repeatStartDate,
	LocalDate repeatEndDate,
	Long parentId,
	Long childId,
	LocalDateTime createdAt
) {

	public static AdminScheduleResponse from(Schedule schedule) {
		return new AdminScheduleResponse(
			schedule.getId(),
			schedule.getName(),
			schedule.getStartTime(),
			schedule.getEndTime(),
			schedule.getScheduleColor().name(),
			schedule.isRecurring(),
			schedule.getRepeatStartDate(),
			schedule.getRepeatEndDate(),
			schedule.getParent().getId(),
			schedule.getChild().getId(),
			schedule.getCreatedAt()
		);
	}
}
