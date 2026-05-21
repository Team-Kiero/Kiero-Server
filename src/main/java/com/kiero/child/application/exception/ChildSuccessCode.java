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
    LOGIN_SUCCESS(HttpStatus.OK, "로그인에 성공하였습니다."),
    GET_INFO_SUCCESS(HttpStatus.OK, "정보 조회에 성공하였습니다."),
    PARENT_WITHDRAWAL_STATUS_RETRIEVED(HttpStatus.OK, "부모 탈퇴 여부가 성공적으로 조회되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}