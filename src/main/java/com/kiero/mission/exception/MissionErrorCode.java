package com.kiero.mission.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MissionErrorCode implements BaseCode {
    /*
    400 BAD_REQUEST : 잘못된 요청 (비즈니스 로직 위반)
    */
    MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 미션입니다."),
    MISSION_EXPIRED(HttpStatus.BAD_REQUEST, "마감 기한이 지난 미션입니다."),

    /*
    403 FORBIDDEN : 권한 없음
    */
    NOT_YOUR_CHILD(HttpStatus.FORBIDDEN, "해당 자녀에 대한 권한이 없습니다."),
    NOT_YOUR_MISSION(HttpStatus.FORBIDDEN, "본인의 미션이 아닙니다."),

    /*
    404 NOT_FOUND : 리소스 없음
    */
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 자녀입니다."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 미션입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
