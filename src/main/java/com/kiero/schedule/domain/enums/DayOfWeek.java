package com.kiero.schedule.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DayOfWeek {
	MON("월요일"),
	TUE("화요일"),
	WED("수요일"),
	THU("목요일"),
	FRI("금요일"),
	SAT("토요일"),
	SUN("일요일")
	;

	private final String dayOfWeek;
}
