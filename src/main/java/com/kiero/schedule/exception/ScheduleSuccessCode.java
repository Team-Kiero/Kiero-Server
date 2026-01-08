package com.kiero.schedule.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleSuccessCode implements BaseCode {
	/*
	200 OK
	 */
	SCHEDULE_TAB_GET_SUCCESS(HttpStatus.OK, "일정탭이 조회되었습니다."),
	TODAY_SCHEDULE_GET_SUCCESS(HttpStatus.OK, "오늘의 일정이 조회되었습니다."),
	/*
	201 CREATED
	 */
	SCHEDULE_CREATED(HttpStatus.CREATED, "일정이 생성되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
