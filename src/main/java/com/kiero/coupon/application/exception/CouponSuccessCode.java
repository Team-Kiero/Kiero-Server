package com.kiero.coupon.application.exception;

import org.springframework.http.HttpStatus;

import com.kiero.global.response.base.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponSuccessCode implements BaseCode {
    /*
    200 OK
    */
    COUPONS_RETRIEVED(HttpStatus.OK, "쿠폰 목록을 조회했습니다."),
    COUPON_PURCHASED(HttpStatus.OK, "쿠폰 구매에 성공하였습니다."),
    COUPON_UPDATED(HttpStatus.OK, "쿠폰이 성공적으로 수정되었습니다."),
    COUPON_DELETED(HttpStatus.OK, "쿠폰이 성공적으로 삭제되었습니다."),

    /*
    201 CREATED
    */
    COUPON_CREATED(HttpStatus.CREATED, "쿠폰이 성공적으로 생성되었습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
