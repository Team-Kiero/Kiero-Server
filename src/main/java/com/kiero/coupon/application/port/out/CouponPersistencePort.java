package com.kiero.coupon.application.port.out;

import com.kiero.coupon.domain.Coupon;

public interface CouponPersistencePort {
	Coupon save(Coupon coupon);
	void delete(Coupon coupon);
}
