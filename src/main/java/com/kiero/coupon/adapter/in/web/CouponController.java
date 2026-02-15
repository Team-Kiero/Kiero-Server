package com.kiero.coupon.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.coupon.application.exception.CouponSuccessCode;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.PurchaseCouponCommand;
import com.kiero.coupon.application.port.in.GetCouponsUseCase;
import com.kiero.coupon.application.port.in.PurchaseCouponUseCase;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

	private final GetCouponsUseCase getCouponsUseCase;
	private final PurchaseCouponUseCase purchaseCouponUseCase;

	@PreAuthorize("hasAnyRole('CHILD', 'PARENT', 'ADMIN')")
	@GetMapping
	public ResponseEntity<SuccessResponse<List<CouponResponse>>> getAllCoupons() {

		List<CouponResponse> coupons = getCouponsUseCase.getAll()
			.stream()
			.map(dto -> new CouponResponse(dto.couponId(), dto.name(), dto.price()))
			.toList();

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPONS_RETRIEVED, coupons));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PatchMapping("/{couponId}")
	public ResponseEntity<SuccessResponse<CouponResponse>> purchaseCoupon(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long couponId
	) {
		CouponResponse dto = purchaseCouponUseCase.purchase(
			new PurchaseCouponCommand(currentAuth.memberId(), couponId)
		);

		CouponResponse response = new CouponResponse(dto.couponId(), dto.name(), dto.price());

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPON_PURCHASED, response));
	}
}