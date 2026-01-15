package com.kiero.schedule.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements BaseCode {
	/*
	400 BAD REQUEST
	 */
	INVALID_DAY_OF_WEEK(HttpStatus.BAD_REQUEST, "형식에 맞지 않는 요일 입력입니다."),
	FIRE_LIT_ALREADY_COMPLETE(HttpStatus.BAD_REQUEST, "오늘의 불 피우기가 이미 완료되었습니다."),
	SCHEDULE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "해당 일정은 이미 완료되었습니다."),
	SCHEDULE_COULD_NOT_BE_SKIPPED(HttpStatus.BAD_REQUEST, "해당 일정은 다음으로 넘어갈 수 있는 상태가 아닙니다."),
	DAY_OF_WEEK_XOR_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "반복 요일이나 일정 중 한 가지만 입력되어야 합니다."),
	DAY_OF_WEEK_NOT_NULLABLE_WHEN_IS_RECURRING_IS_TRUE(HttpStatus.BAD_REQUEST, "반복 일정일 때, 반복요일이 입력되어야 합니다."),
	DATE_NOT_NULLABLE_WHEN_IS_RECURRING_IS_FALSE(HttpStatus.BAD_REQUEST, "반복 일정이 아닐 때, 일정 일자가 입력되어야 합니다."),
	INVALID_DATE_DURATION(HttpStatus.BAD_REQUEST, "시작 일자가 종료 일자의 이전 시점이어야 합니다."),

	/*
	403 FORBIDDEN
	 */
	SCHEDULE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 일정에 접근할 권한이 없습니다."),

	/*
	404 NOT FOUND
	 */
	SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 일정 데이터를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
