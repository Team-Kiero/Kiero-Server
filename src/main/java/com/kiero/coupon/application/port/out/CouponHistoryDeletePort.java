package com.kiero.coupon.application.port.out;

public interface CouponHistoryDeletePort {
	void deleteAllByChildId(Long childId);
}
