package com.kiero.child.exception;

import com.kiero.global.response.base.BaseCode;
import com.kiero.global.response.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChildErrorCode implements BaseCode {
    /*
    404 NOT_FOUND
    */
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 자녀입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
