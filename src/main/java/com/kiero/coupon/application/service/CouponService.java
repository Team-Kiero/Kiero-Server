package com.kiero.coupon.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.application.port.out.ChildLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.coupon.application.dto.CouponPurchaseEvent;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.PurchaseCouponCommand;
import com.kiero.coupon.application.exception.CouponErrorCode;
import com.kiero.coupon.application.port.in.GetCouponsUseCase;
import com.kiero.coupon.application.port.in.PurchaseCouponUseCase;
import com.kiero.coupon.application.port.out.CouponLoadPort;
import com.kiero.coupon.application.port.out.CouponPurchaseEventPort;
import com.kiero.coupon.domain.Coupon;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService implements GetCouponsUseCase, PurchaseCouponUseCase {

	private final ChildLoadPort childLoadPort;

	private final CouponLoadPort couponLoadPort;
	private final CouponPurchaseEventPort eventPort;

	@Override
	@Transactional(readOnly = true)
	public List<CouponResponse> getAll() {
		return couponLoadPort.findAllOrderByPriceAsc()
			.stream()
			.map(c -> new CouponResponse(c.getId(), c.getName(), c.getPrice()))
			.toList();
	}

	@Override
	@Transactional
	public CouponResponse purchase(PurchaseCouponCommand command) {

		Child child = childLoadPort.findByIdWithLock(command.childId())
			.orElseThrow(() -> new KieroException(CouponErrorCode.CHILD_NOT_FOUND));

		Coupon coupon = couponLoadPort.findById(command.couponId())
			.orElseThrow(() -> new KieroException(CouponErrorCode.COUPON_NOT_FOUND));

		if (!child.hasEnoughCoin(coupon.getPrice())) {
			throw new KieroException(CouponErrorCode.INSUFFICIENT_COINS);
		}

		child.deductCoin(coupon.getPrice());

		eventPort.publish(new CouponPurchaseEvent(
			child.getId(),
			coupon.getName(),
			coupon.getPrice(),
			LocalDateTime.now()
		));

		return new CouponResponse(coupon.getId(), coupon.getName(), coupon.getPrice());
	}
}
