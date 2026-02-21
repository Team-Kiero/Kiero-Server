package com.kiero.coupon.application.dto;

public record UpdateCouponCommand(
	Long parentId,
	Long couponId,
	String name,
	int price
) {
}
