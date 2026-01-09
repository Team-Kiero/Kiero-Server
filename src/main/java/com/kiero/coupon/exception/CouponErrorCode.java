package com.kiero.coupon.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

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
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
