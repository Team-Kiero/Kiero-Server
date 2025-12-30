package com.kiero.global.auth.jwt.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements BaseCode {

	/*
	400 BAD REQUEST
	 */
	INVALID_REFRESH_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "잘못된 리프레쉬 토큰입니다"),
	REFRESH_TOKEN_MEMBER_ID_MISMATCH_ERROR(HttpStatus.BAD_REQUEST, "리프레쉬 토큰의 사용자 정보가 일치하지 않습니다"),
	UNSUPPORTED_REFRESH_TOKEN_ERROR(HttpStatus.BAD_REQUEST, "지원하지 않는 리프레쉬 토큰입니다"),
	REFRESH_TOKEN_EMPTY_ERROR(HttpStatus.BAD_REQUEST, "리프레쉬 토큰이 비어있습니다"),
	REFRESH_TOKEN_SIGNATURE_ERROR(HttpStatus.BAD_REQUEST, "리프레쉬 토큰의 서명의 잘못 되었습니다"),

	/*
	401 UNAUTHORIZED
	 */
	AUTHENTICATION_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "인가코드가 만료되었습니다"),
	REFRESH_TOKEN_EXPIRED_ERROR(HttpStatus.UNAUTHORIZED, "리프레쉬 토큰이 만료되었습니다"),
	INVALID_AUTHORIZATION_HEADER(HttpStatus.UNAUTHORIZED, "유효하지 않은 authorization 헤더입니다"),

	/*
	 404 NOT FOUND
	 */
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레쉬 토큰이 존재하지 않습니다"),

	/*
		500 INTERNAL SERVER ERROR
	 */
	UNKNOWN_REFRESH_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 리프레쉬 토큰 오류가 발생했습니다");

	private final HttpStatus httpStatus;
	private final String message;
}