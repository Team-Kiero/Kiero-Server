package com.kiero.admin.application.dto;

import java.time.LocalDateTime;

import com.kiero.coupon.domain.Coupon;

public record AdminCouponResponse(
	Long id,
	String name,
	int price,
	Long parentId,
	Long childId,
	LocalDateTime createdAt
) {

	public static AdminCouponResponse from(Coupon coupon) {
		return new AdminCouponResponse(
			coupon.getId(),
			coupon.getName(),
			coupon.getPrice(),
			coupon.getParent().getId(),
			coupon.getChild().getId(),
			coupon.getCreatedAt()
		);
	}
}
