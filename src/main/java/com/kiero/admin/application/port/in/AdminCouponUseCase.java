package com.kiero.admin.application.port.in;

import java.util.List;

import com.kiero.admin.application.dto.AdminCouponResponse;
import com.kiero.admin.application.dto.AdminCouponUpdateRequest;

public interface AdminCouponUseCase {
	List<AdminCouponResponse> findAllByChildId(Long childId);
	AdminCouponResponse updateCoupon(Long couponId, AdminCouponUpdateRequest request);
	void deleteCoupon(Long couponId);
}
