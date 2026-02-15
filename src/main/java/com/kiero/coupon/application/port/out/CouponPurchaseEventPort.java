package com.kiero.coupon.application.port.out;

import com.kiero.coupon.application.dto.CouponPurchaseEvent;

public interface CouponPurchaseEventPort {
	void publish(CouponPurchaseEvent event);
}