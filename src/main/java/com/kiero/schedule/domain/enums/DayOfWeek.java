package com.kiero.schedule.domain.enums;

import com.kiero.global.exception.KieroException;
import com.kiero.schedule.application.exception.ScheduleErrorCode;

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

	public static DayOfWeek from(java.time.DayOfWeek javaDayOfWeek) {
		return DayOfWeek.valueOf(javaDayOfWeek.name().substring(0, 3));
	}

	public static java.time.DayOfWeek toJavaDayOfWeek(DayOfWeek dayOfWeek) {
		switch (dayOfWeek) {
			case MON -> { return java.time.DayOfWeek.MONDAY; }
			case TUE -> { return java.time.DayOfWeek.TUESDAY; }
			case WED -> { return java.time.DayOfWeek.WEDNESDAY; }
			case THU -> { return java.time.DayOfWeek.THURSDAY; }
			case FRI -> { return java.time.DayOfWeek.FRIDAY; }
			case SAT -> { return java.time.DayOfWeek.SATURDAY; }
			case SUN -> { return java.time.DayOfWeek.SUNDAY; }
		}
		throw new KieroException(ScheduleErrorCode.INTERNAL_SERVER_ERROR);
	}
}
