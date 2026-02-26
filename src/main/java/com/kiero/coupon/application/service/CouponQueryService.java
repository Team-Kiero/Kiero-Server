package com.kiero.coupon.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.port.in.CouponsQueryUseCase;
import com.kiero.coupon.application.port.out.CouponLoadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponQueryService implements CouponsQueryUseCase {

	private final CouponLoadPort couponLoadPort;

	@Override
	@Transactional(readOnly = true)
	public List<CouponResponse> getAll() {
		return couponLoadPort.findAllOrderByPriceAsc()
			.stream()
			.map(c -> new CouponResponse(c.getId(), c.getName(), c.getPrice()))
			.toList();
	}
}
