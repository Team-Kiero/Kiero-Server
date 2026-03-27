package com.kiero.coupon.application.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.port.out.CouponLoadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponCacheableService {

	private final CouponLoadPort couponLoadPort;

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "coupons", key = "#childId + ':' + T(java.time.LocalDate).now()", condition = "#childId != null")
	public List<CouponResponse> getCouponsByChild(Long childId) {
		return couponLoadPort.findAllByChildIdOrderByPriceAsc(childId)
			.stream()
			.map(CouponResponse::from)
			.toList();
	}
}
