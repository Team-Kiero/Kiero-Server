package com.kiero.coupon.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record CouponPurchaseEventForFeed(
	List<Parent> parents,
	Long childId,
	Long couponId,
	String name,
	Integer amount,
	LocalDateTime occurredAt
) {
}