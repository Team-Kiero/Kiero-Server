package com.kiero.coupon.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements BaseCode {
    /*
    400 Bad Request
    */
    INSUFFICIENT_COINS(HttpStatus.BAD_REQUEST, "금화가 부족합니다."),

    /*
    404 Not Found
    */
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "아이 정보를 찾을 수 없습니다."),

    /*
    403 Forbidden
    */
    NOT_YOUR_CHILD(HttpStatus.FORBIDDEN, "해당 아이의 부모가 아닙니다."),
    NOT_YOUR_COUPON(HttpStatus.FORBIDDEN, "해당 쿠폰에 대한 권한이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
