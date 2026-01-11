package com.kiero.coupon.presentation.dto;

import java.time.LocalDateTime;

public record CouponPurchaseEvent(
	Long childId,
	String name,
	Integer amount,
	LocalDateTime occurredAt
) {
}
