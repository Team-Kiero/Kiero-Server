package com.kiero.feed.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedSuccessCode implements BaseCode {
	/*
	200 OK
	 */
	FEED_GET_SUCCESS(HttpStatus.OK, "피드 조회가 성공하였습니다.");
	;

	private final HttpStatus httpStatus;
	private final String message;
}
