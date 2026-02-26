package com.kiero.coupon.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.exception.CouponErrorCode;
import com.kiero.coupon.application.port.in.CouponsQueryUseCase;
import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponQueryService implements CouponsQueryUseCase {

	private final CouponLoadPort couponLoadPort;
	private final ParentChildAccessPort parentChildAccessPort;

	@Override
	@Transactional(readOnly = true)
	public List<CouponResponse> getCouponsByChild(Long childId) {
		return couponLoadPort.findAllByChildIdOrderByPriceAsc(childId)
			.stream()
			.map(CouponResponse::from)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CouponResponse> getCouponsByParent(Long parentId, Long childId) {
		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_CHILD);
		}

		return couponLoadPort.findAllByChildIdOrderByPriceAsc(childId)
			.stream()
			.map(CouponResponse::from)
			.toList();
	}
}
