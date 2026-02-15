package com.kiero.child.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChildSuccessCode implements BaseCode {
    /*
    200 OK
    */
    GET_INFO_SUCCESS(HttpStatus.OK, "정보 조회에 성공하였습니다."),

    /*
    201 CREATED
    */
    SIGNUP_SUCCESS(HttpStatus.CREATED, "가입에 성공하였습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}