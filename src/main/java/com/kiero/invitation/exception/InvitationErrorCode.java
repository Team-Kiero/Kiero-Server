package com.kiero.invitation.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InvitationErrorCode implements BaseCode {
    /*
    400 Bad Request
     */
    INVALID_OR_EXPIRED_INVITE_CODE(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 초대 코드입니다."),
    INVITE_CODE_NAME_MISMATCH(HttpStatus.BAD_REQUEST, "초대 코드에 등록된 자녀 이름과 일치하지 않습니다."),

    /*
	500 Internal Server Error
 	*/
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대코드 생성에 실패했습니다."),
    PARENT_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드와 연결된 부모를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
