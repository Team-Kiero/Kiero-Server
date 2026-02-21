package com.kiero.coupon.application.dto;

import java.time.LocalDateTime;

public record CouponPurchaseEvent(
	Long childId,
	String name,
	Integer amount,
	LocalDateTime occurredAt
) {
}