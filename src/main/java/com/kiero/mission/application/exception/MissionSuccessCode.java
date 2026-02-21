package com.kiero.mission.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionSuccessCode implements BaseCode {
    /*
    200 OK
    */
    MISSIONS_RETRIEVED(HttpStatus.OK, "미션 목록 조회에 성공하였습니다."),
    MISSION_UPDATED(HttpStatus.OK, "미션이 성공적으로 수정되었습니다."),
    MISSION_COMPLETED(HttpStatus.OK, "미션을 성공적으로 완료했습니다."),
    MISSION_SUGGESTIONS_GENERATED(HttpStatus.OK, "미션 추천이 생성되었습니다."),

    /*
    201 CREATED
    */
    MISSION_CREATED(HttpStatus.CREATED, "미션이 생성되었습니다."),
    MISSIONS_BULK_CREATED(HttpStatus.CREATED, "미션이 일괄 생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
