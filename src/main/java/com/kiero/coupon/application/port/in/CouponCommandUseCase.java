package com.kiero.coupon.application.port.in;

import com.kiero.coupon.application.dto.CouponCreateRequest;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.CouponUpdateRequest;

public interface CouponCommandUseCase {
	CouponResponse createCoupon(Long parentId, Long childId, CouponCreateRequest request);
	CouponResponse updateCoupon(Long parentId, Long couponId, CouponUpdateRequest request);
	void deleteCoupon(Long parentId, Long couponId);
}
