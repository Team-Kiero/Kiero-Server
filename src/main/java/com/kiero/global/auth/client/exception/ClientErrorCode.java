package com.kiero.global.auth.client.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClientErrorCode implements BaseCode {
	/*
	400 Bad Request
 	*/
	PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 타입입니다."),
	PLATFORM_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 플랫폼 환경입니다.")
	;

	private final HttpStatus httpStatus;
	private final String message;

}
