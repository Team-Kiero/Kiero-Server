package com.kiero.terms.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsSuccessCode implements BaseCode {
	/*
	200 OK
	*/
	REQUIRED_TERMS_GET_SUCCESS(HttpStatus.OK, "필수 동의약관이 성공적으로 조회되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
