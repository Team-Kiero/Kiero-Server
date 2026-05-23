package com.kiero.coupon.application.dto;

import java.time.LocalDate;

import com.kiero.coupon.domain.CouponHistory;

public record CouponHistoryResponse(
	String name,
	int price,
	LocalDate purchasedAt
) {
	public static CouponHistoryResponse from(CouponHistory couponHistory) {
		return new CouponHistoryResponse(
			couponHistory.getName(),
			couponHistory.getPrice(),
			couponHistory.getCreatedAt().toLocalDate()
		);
	}
}
