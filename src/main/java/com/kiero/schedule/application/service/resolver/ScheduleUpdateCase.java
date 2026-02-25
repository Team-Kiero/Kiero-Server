package com.kiero.schedule.application.service.resolver;

public enum ScheduleUpdateCase {
	// 단일 일정 -> 단일 일정
	NormalToNormal,

	// 단일 일정 -> 반복 일정
	NormalToRecurring,

	// 반복 일정 -> 반복 일정, 요일 변경 있음
	RecurringToRecurring,

	// 반복 일정 -> 반복 일정, 요일 변경 없음, 이후 일정 포함
	RecurringToRecurringIncludeFollowing,

	// 반복 일정 -> 반복 일정, 요일 변경 없음, 이번 일정만
	RecurringToRecurringExceptFollowing,

	// 반복일정 -> 단일 일정
	RecurringToNormal
}
