package com.kiero.coupon.application.port.in;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.CreateCouponCommand;

public interface CouponCommandUseCase {
	CouponResponse create(CreateCouponCommand command);
}
