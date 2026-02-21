package com.kiero.coupon.application.dto;

public record PurchaseCouponCommand (
	Long childId,
	Long couponId
) {}
