package com.kiero.coupon.application.port.in;

import java.util.List;

import com.kiero.coupon.application.dto.CouponHistoryResponse;
import com.kiero.coupon.application.dto.CouponResponse;

public interface CouponQueryUseCase {
	List<CouponResponse> getCouponsByChild(Long childId);
	List<CouponResponse> getCouponsByParent(Long parentId, Long childId);
	List<CouponHistoryResponse> getCouponHistory(Long childId);
}
