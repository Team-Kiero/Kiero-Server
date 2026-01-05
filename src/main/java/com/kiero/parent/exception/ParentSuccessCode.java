package com.kiero.parent.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParentSuccessCode implements BaseCode {
	/*
	200 OK
	*/
	LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공하였습니다."),
	GET_CHILDREN_SUCCESS(HttpStatus.OK, "자녀 목록 조회에 성공하였습니다."),

    /*
    201 CREATED
    */
    INVITE_CODE_CREATED(HttpStatus.CREATED, "초대 코드가 생성되었습니다."),
	;

	private final HttpStatus httpStatus;
	private final String message;
}
