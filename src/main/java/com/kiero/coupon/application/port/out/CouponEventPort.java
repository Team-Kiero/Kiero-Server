package com.kiero.coupon.application.port.out;

import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.coupon.application.dto.CouponPurchaseEvent;

public interface CouponEventPort {
	void publish(CouponCreatedEvent event);
	void publish(CouponPurchaseEvent event);
}
