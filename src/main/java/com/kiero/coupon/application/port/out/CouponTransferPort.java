package com.kiero.coupon.application.port.out;

public interface CouponTransferPort {
	void transferOwnership(Long fromParentId, Long toParentId, Long childId);
}
