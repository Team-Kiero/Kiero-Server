package com.kiero.coupon.application.dto;

import java.util.List;

public record CouponPurchasedEvent(
	List<Long> parentIds,
	Long childId,
	Long couponId,
	String couponName
) {
}
