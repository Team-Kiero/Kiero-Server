package com.kiero.coupon.application.dto;

public record CouponCreatedEvent(
	Long childId,
	String couponName,
	int price
) {
}
