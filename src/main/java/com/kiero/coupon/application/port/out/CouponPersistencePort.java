package com.kiero.coupon.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.coupon.domain.Coupon;

public interface CouponPersistencePort {
	Coupon save(Coupon coupon);
	void delete(Coupon coupon);
	List<Coupon> findAllByChildIdOrderByPriceAsc(Long childId);
	Optional<Coupon> findById(Long couponId);
}
