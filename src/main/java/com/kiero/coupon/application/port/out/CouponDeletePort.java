package com.kiero.coupon.application.port.out;

public interface CouponDeletePort {
	void deleteAllByChildId(Long childId);
}
