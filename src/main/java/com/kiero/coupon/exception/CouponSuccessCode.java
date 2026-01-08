package com.kiero.coupon.exception;

import com.kiero.global.response.base.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CouponSuccessCode implements BaseCode {
    /*
    200 OK
    */
    COUPONS_RETRIEVED(HttpStatus.OK, "쿠폰 목록을 조회했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
