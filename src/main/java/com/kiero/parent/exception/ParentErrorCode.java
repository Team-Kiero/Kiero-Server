package com.kiero.parent.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ParentErrorCode implements BaseCode {
    /*
    404 NOT_FOUND
    */
    PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 부모입니다."),
    ;
    
    private final HttpStatus httpStatus;
    private final String message;
}
