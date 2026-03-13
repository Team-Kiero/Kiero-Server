package com.kiero.coupon.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.coupon.application.port.out.CouponPersistencePort;
import com.kiero.coupon.domain.Coupon;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponPersistenceAdapter implements CouponLoadPort, CouponPersistencePort {

	private final CouponRepository couponRepository;

	@Override
	public List<Coupon> findAllByChildIdOrderByPriceAsc(Long childId) {
		return couponRepository.findAllByChildIdOrderByPriceAscCreatedAtDesc(childId);
	}

	@Override
	public Optional<Coupon> findById(Long couponId) {
		return couponRepository.findById(couponId);
	}

	@Override
	public Coupon save(Coupon coupon) {
		return couponRepository.save(coupon);
	}

	@Override
	public void delete(Coupon coupon) {
		couponRepository.delete(coupon);
	}
}