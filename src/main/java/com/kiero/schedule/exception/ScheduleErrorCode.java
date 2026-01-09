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
