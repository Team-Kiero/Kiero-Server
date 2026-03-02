package com.kiero.coupon.application.port.out;

import com.kiero.coupon.application.dto.CouponCreatedEvent;
import com.kiero.coupon.application.dto.CouponPurchaseEventForFeed;

public interface CouponEventPort {
	void publish(CouponCreatedEvent event);
	void publish(CouponPurchaseEventForFeed event);
}
