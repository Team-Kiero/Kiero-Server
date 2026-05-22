package com.kiero.coupon.application.port.out;

import java.util.List;

import com.kiero.coupon.domain.CouponHistory;

public interface CouponHistoryPersistencePort {
	CouponHistory save(CouponHistory couponHistory);
	List<CouponHistory> findAllByChildIdCreatedAtDesc(Long childId);
}