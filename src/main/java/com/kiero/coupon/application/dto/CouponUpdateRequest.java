package com.kiero.coupon.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CouponUpdateRequest(
	@NotBlank(message = "쿠폰 이름은 필수입니다.")
	String name,
	@Positive(message = "가격은 1 이상이어야 합니다.")
	int price
) {
}
