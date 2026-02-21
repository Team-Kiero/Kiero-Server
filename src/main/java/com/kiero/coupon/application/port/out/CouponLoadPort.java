package com.kiero.coupon.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.coupon.domain.Coupon;

public interface CouponLoadPort {
	List<Coupon> findAllOrderByPriceAsc();
	Optional<Coupon> findById(Long couponId);
}