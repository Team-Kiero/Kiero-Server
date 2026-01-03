package com.kiero.mission.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseCode {
    /*
    200 OK
    */
    MISSIONS_RETRIEVED(HttpStatus.OK, "미션 목록 조회에 성공하였습니다."),

    /*
    201 CREATED
    */
    MISSION_CREATED(HttpStatus.CREATED, "미션이 생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
