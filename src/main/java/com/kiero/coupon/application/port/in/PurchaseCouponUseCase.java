package com.kiero.coupon.application.port.in;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.PurchaseCouponCommand;

public interface PurchaseCouponUseCase {
	CouponResponse purchase(PurchaseCouponCommand command);
}
