package com.kiero.coupon.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.exception.CouponErrorCode;
import com.kiero.coupon.application.port.in.CouponQueryUseCase;
import com.kiero.coupon.application.port.out.CouponHistoryPersistencePort;
import com.kiero.coupon.domain.CouponHistory;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildAccessPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponQueryService implements CouponQueryUseCase {

	private final CouponCacheableService couponCacheableService;
	private final ParentChildAccessPort parentChildAccessPort;

	@Override
	public List<CouponResponse> getCouponsByChild(Long childId) {
		return couponCacheableService.getCouponsByChild(childId);
	}

	@Override
	public List<CouponResponse> getCouponsByParent(Long parentId, Long childId) {
		if (!parentChildAccessPort.existsByParentIdAndChildId(parentId, childId)) {
			throw new KieroException(CouponErrorCode.NOT_YOUR_CHILD);
		}
		return couponCacheableService.getCouponsByChild(childId);
	}
}
