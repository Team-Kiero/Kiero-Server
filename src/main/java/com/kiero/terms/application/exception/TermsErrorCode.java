package com.kiero.terms.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsErrorCode implements BaseCode {
	/*
	400 Bad Request
	*/
	DUPLICATE_TERMS_ID(HttpStatus.BAD_REQUEST, "약관 목록에 중복된 항목이 있습니다."),
	ALREADY_AGREED_TERMS(HttpStatus.BAD_REQUEST, "이미 동의한 약관입니다."),

	/*
	404 Not Found
	*/
	TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "약관을 찾을 수 없습니다."),
	PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
