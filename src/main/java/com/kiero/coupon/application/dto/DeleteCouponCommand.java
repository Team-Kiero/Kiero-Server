package com.kiero.coupon.application.dto;

public record DeleteCouponCommand(
	Long parentId,
	Long couponId
) {
}
