package com.kiero.admin.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminSuccessCode implements BaseCode {

	/*
	200 OK
	 */
	LOGIN_SUCCESS(HttpStatus.OK, "관리자 로그인에 성공하였습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
