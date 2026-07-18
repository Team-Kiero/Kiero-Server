package com.kiero.feed.application.exception;

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
	404 NOT FOUND
	 */
	FEED_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "피드 아이템을 찾을 수 없습니다."),

	/*
	500 INTERNAL SERVER ERROR
	 */
	JSON_CONVERT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "json 형식 변환 과정이 실패하였습니다.")
	;

	private final HttpStatus httpStatus;
	private final String message;
}
