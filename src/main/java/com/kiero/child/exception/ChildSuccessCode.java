package com.kiero.child.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChildSuccessCode implements BaseCode {
    /*
    201 CREATED
    */
    SIGNUP_SUCCESS(HttpStatus.CREATED, "가입에 성공하였습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}