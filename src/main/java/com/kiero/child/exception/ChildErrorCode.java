package com.kiero.child.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChildErrorCode implements BaseCode {
	/*
	404 NOT FOUND
	 */
	CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "아이 정보를 찾을 수 없습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;

}