package com.kiero.feed.application.exception;

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
	FEED_GET_SUCCESS(HttpStatus.OK, "피드 조회가 성공하였습니다."),
	FEED_HAS_UNREAD_GET_SUCCESS(HttpStatus.OK, "미확인 알림 존재 여부를 조회하였습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
