package com.kiero.global.auth.client.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OAuthErrorCode implements BaseCode {

	/*
	401 UNAUTHORIZED
 	*/
	O_AUTH_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "OAuth 인증에 접근할 수 없습니다."),
	GET_INFO_ERROR(HttpStatus.UNAUTHORIZED, "사용자의 정보를 가져올 수 없습니다."),
	INVALID_APPLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "Apple identity token 검증에 실패했습니다."),
	APPLE_CLIENT_SECRET_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Apple client secret 생성에 실패했습니다."),
	APPLE_AUTH_CODE_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "Apple authorization code 교환에 실패했습니다."),
	INVALID_APPLE_EVENT_TOKEN(HttpStatus.UNAUTHORIZED, "Apple 서버 이벤트 token 검증에 실패했습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
