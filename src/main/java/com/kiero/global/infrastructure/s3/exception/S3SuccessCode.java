package com.kiero.global.infrastructure.s3.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum S3SuccessCode implements BaseCode {

    PRESIGNED_URL_CREATED(HttpStatus.OK, "Presigned URL이 생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
