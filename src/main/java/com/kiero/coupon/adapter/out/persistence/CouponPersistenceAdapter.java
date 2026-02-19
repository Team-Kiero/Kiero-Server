package com.kiero.coupon.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.coupon.domain.Coupon;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponPersistenceAdapter implements CouponLoadPort {

	private final CouponRepository couponRepository;

	@Override
	public List<Coupon> findAllOrderByPriceAsc() {
		return couponRepository.findAllByOrderByPriceAsc();
	}

	@Override
	public Optional<Coupon> findById(Long couponId) {
		return couponRepository.findById(couponId);
	}
}