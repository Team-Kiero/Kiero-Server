package com.kiero.admin.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminCouponLoadPort;
import com.kiero.coupon.adapter.out.persistence.CouponRepository;
import com.kiero.coupon.domain.Coupon;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminCouponPersistenceAdapter implements AdminCouponLoadPort {

	private final CouponRepository couponRepository;

	@Override
	public List<Coupon> findAllByChildId(Long childId) {
		return couponRepository.findAllByChildId(childId);
	}

	@Override
	public Optional<Coupon> findById(Long couponId) {
		return couponRepository.findById(couponId);
	}
}
