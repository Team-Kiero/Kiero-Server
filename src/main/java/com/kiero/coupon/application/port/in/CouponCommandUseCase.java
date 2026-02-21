package com.kiero.coupon.application.port.in;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.CreateCouponCommand;
import com.kiero.coupon.application.dto.UpdateCouponCommand;

public interface CouponCommandUseCase {
	CouponResponse create(CreateCouponCommand command);
	CouponResponse update(UpdateCouponCommand command);
}
