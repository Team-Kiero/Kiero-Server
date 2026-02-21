package com.kiero.coupon.application.port.in;

import java.util.List;

import com.kiero.coupon.application.dto.CouponResponse;

public interface GetCouponsUseCase {
	List<CouponResponse> getAll();
}
