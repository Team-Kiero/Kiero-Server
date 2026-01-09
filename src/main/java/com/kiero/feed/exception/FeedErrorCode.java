package com.kiero.feed.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements BaseCode {
	/*
	400 BAD REQUEST
	 */
	CURSOR_NOT_VALID(HttpStatus.BAD_REQUEST, "커서 입력값이 올바르지 않습니다."),

	/*
	500 INTERNAL SERVER ERROR
	 */
	JSON_CONVERT_FAILED(HttpStatus.BAD_REQUEST, "json 형식 변환 과정이 실패하였습니다.")
	;

	private final HttpStatus httpStatus;
	private final String message;
}
