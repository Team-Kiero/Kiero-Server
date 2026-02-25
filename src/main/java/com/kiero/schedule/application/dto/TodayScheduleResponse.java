package com.kiero.schedule.application.dto;

import java.time.LocalTime;

import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.application.service.resolver.TodayScheduleStatus;

public record TodayScheduleResponse(
	Long scheduleDetailId,
	int scheduleOrder,
	LocalTime startTime,
	LocalTime endTime,
	String name,
	StoneType stoneType,
	int totalSchedule,
	int earnedStones,
	TodayScheduleStatus scheduleStatus,
	boolean isSkippable,
	boolean isNowScheduleVerified
) {
	public static TodayScheduleResponse of(
		Long scheduleDetailId,
		int scheduleOrder,
		LocalTime startTime,
		LocalTime endTime,
		String name,
		StoneType stoneType,
		int totalSchedule,
		int earnedStones,
		TodayScheduleStatus scheduleStatus,
		boolean isSkippable,
		boolean isNowScheduleVerified
	) {
		return new TodayScheduleResponse(
			scheduleDetailId,
			scheduleOrder,
			startTime,
			endTime,
			name,
			stoneType,
			totalSchedule,
			earnedStones,
			scheduleStatus,
			isSkippable,
			isNowScheduleVerified);
	}
}
