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
	SUN("일요일");

	private final String dayOfWeek;

	public static DayOfWeek from(java.time.DayOfWeek javaDayOfWeek) {
		return DayOfWeek.valueOf(javaDayOfWeek.name().substring(0, 3));
	}

	public static java.time.DayOfWeek toJavaDayOfWeek(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MON -> java.time.DayOfWeek.MONDAY;
			case TUE -> java.time.DayOfWeek.TUESDAY;
			case WED -> java.time.DayOfWeek.WEDNESDAY;
			case THU -> java.time.DayOfWeek.THURSDAY;
			case FRI -> java.time.DayOfWeek.FRIDAY;
			case SAT -> java.time.DayOfWeek.SATURDAY;
			case SUN -> java.time.DayOfWeek.SUNDAY;
		};
	}
}
