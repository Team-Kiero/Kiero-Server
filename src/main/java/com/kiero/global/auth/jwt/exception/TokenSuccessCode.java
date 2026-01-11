package com.kiero.global.auth.jwt.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenSuccessCode implements BaseCode {
	/*
	200 OK
 	*/
	LOGOUT_SUCCESS(HttpStatus.OK, "로그아웃에 성공하였습니다."),
	ACCESS_TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "액세스 토큰 재발행에 성공하였습니다."),
	TOKENS_REISSUE_SUCCESS(HttpStatus.OK, "액세스 토큰 및 리프레쉬 토큰 재발행에 성공하였습니다."),
	SUBSCRIBE_TOKEN_ISSUE_SUCCESS(HttpStatus.OK, "sse 구독용 액세스 토큰 발행에 성공하였습니다"),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
