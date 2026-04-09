package com.kiero.admin.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.dto.AdminCouponResponse;
import com.kiero.admin.application.dto.AdminCouponUpdateRequest;
import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminCouponUseCase;
import com.kiero.admin.application.port.out.AdminCouponLoadPort;
import com.kiero.admin.application.port.out.AdminDeletePort;
import com.kiero.coupon.domain.Coupon;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCouponService implements AdminCouponUseCase {

	private final AdminCouponLoadPort adminCouponLoadPort;
	private final AdminDeletePort adminDeletePort;

	@Override
	@Transactional(readOnly = true)
	public List<AdminCouponResponse> findAllByChildId(Long childId) {
		return adminCouponLoadPort.findAllByChildId(childId)
			.stream()
			.map(AdminCouponResponse::from)
			.toList();
	}

	@Override
	@Transactional
	public AdminCouponResponse updateCoupon(Long couponId, AdminCouponUpdateRequest request) {
		Coupon coupon = adminCouponLoadPort.findById(couponId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.COUPON_NOT_FOUND));

		coupon.update(request.name(), request.price());

		return AdminCouponResponse.from(coupon);
	}

	@Override
	@Transactional
	public void deleteCoupon(Long couponId) {
		adminCouponLoadPort.findById(couponId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.COUPON_NOT_FOUND));

		adminDeletePort.deleteCouponById(couponId);
	}
}
