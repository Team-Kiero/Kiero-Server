package com.kiero.admin.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.coupon.domain.Coupon;

public interface AdminCouponLoadPort {
	List<Coupon> findAllByChildId(Long childId);
	Optional<Coupon> findById(Long couponId);
}
