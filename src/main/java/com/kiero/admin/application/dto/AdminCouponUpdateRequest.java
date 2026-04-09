package com.kiero.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AdminCouponUpdateRequest(
	@NotBlank(message = "쿠폰 이름은 필수입니다.")
	String name,

	@Positive(message = "쿠폰 가격은 양수여야 합니다.")
	int price
) {
}
