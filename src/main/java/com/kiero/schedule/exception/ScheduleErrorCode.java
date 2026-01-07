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
	;

	private final HttpStatus httpStatus;
	private final String message;
}
