package com.kiero.mission.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseCode {
    /*
    403 FORBIDDEN
    */
    NOT_YOUR_CHILD(HttpStatus.FORBIDDEN, "해당 자녀에 대한 권한이 없습니다."),

    /*
    404 NOT_FOUND
    */
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 자녀입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
