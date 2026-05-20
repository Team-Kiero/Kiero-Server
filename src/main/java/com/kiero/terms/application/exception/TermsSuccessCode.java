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
	REQUIRED_TERMS_AGREEMENT_STATUS_RETRIEVED(HttpStatus.OK, "필수 동의약관 동의 여부가 성공적으로 조회되었습니다."),

	/*
	201 CREATED
	*/
	TERMS_AGREEMENT_CREATED(HttpStatus.CREATED, "동의 내역 성공적으로 저장되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
