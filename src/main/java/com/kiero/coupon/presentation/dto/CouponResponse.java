package com.kiero.coupon.presentation.dto;

import com.kiero.coupon.domain.Coupon;

public record CouponResponse(
        Long couponId,
        String name,
        int price
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getPrice()
        );
    }
}
