package com.kiero.parent.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParentErrorCode implements BaseCode {
	/*
	404 NOT FOUND
	 */
	PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 정보를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;

}
