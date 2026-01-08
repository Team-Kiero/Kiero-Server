package com.kiero.schedule.presentation.dto;

import java.time.LocalTime;

import com.kiero.schedule.domain.enums.StoneType;
import com.kiero.schedule.domain.enums.TodayScheduleStatus;

public record TodayScheduleResponse(
	Long scheduleDetailId,
	LocalTime startTime,
	LocalTime endTime,
	String name,
	StoneType stoneType,
	int totalSchedule,
	int earnedStones,
	TodayScheduleStatus scheduleStatus,
	boolean isNextScheduleExists
) {
	public static TodayScheduleResponse of(
		Long scheduleDetailId,
		LocalTime startTime,
		LocalTime endTime,
		String name,
		StoneType stoneType,
		int totalSchedule,
		int earnedStones,
		TodayScheduleStatus scheduleStatus,
		boolean isNextScheduleExists
	) {
		return new TodayScheduleResponse(
			scheduleDetailId,
			startTime,
			endTime,
			name,
			stoneType,
			totalSchedule,
			earnedStones,
			scheduleStatus,
			isNextScheduleExists);
	}
}
