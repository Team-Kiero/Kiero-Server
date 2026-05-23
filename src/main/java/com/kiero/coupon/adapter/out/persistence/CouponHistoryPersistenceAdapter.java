package com.kiero.coupon.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.coupon.application.port.out.CouponHistoryPersistencePort;
import com.kiero.coupon.domain.CouponHistory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponHistoryPersistenceAdapter implements CouponHistoryPersistencePort {

	private final CouponHistoryRepository couponHistoryRepository;

	@Override
	public CouponHistory save(CouponHistory couponHistory) {
		return couponHistoryRepository.save(couponHistory);
	}

	@Override
	public List<CouponHistory> findAllByChildIdCreatedAtDesc(Long childId) {
		return couponHistoryRepository.findAllByChildIdOrderByCreatedAtDesc(childId);
	}
}
