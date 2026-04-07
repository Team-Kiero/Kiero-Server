package com.kiero.admin.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements BaseCode {

	/*
	401 UNAUTHORIZED
	 */
	INVALID_ADMIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),

	/*
	404 NOT FOUND
	 */
	ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
