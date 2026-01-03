package com.kiero.mission.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseCode {
    /*
    201 CREATED
    */
    MISSION_CREATED(HttpStatus.CREATED, "미션이 생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
