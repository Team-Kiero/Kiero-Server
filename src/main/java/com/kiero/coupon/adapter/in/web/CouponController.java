package com.kiero.coupon.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.coupon.application.dto.CouponCreateRequest;
import com.kiero.coupon.application.dto.CouponHistoryResponse;
import com.kiero.coupon.application.dto.CouponResponse;
import com.kiero.coupon.application.dto.CouponUpdateRequest;
import com.kiero.coupon.application.exception.CouponSuccessCode;
import com.kiero.coupon.application.port.in.CouponCommandUseCase;
import com.kiero.coupon.application.port.in.CouponQueryUseCase;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

	private final CouponQueryUseCase couponQueryUseCase;
	private final CouponCommandUseCase couponCommandUseCase;

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@GetMapping
	public ResponseEntity<SuccessResponse<List<CouponResponse>>> getCouponsByChild(
    @CurrentMember CurrentAuth currentAuth
  ) {
    List<CouponResponse> response = couponQueryUseCase.getCouponsByChild(currentAuth.memberId());

    return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPONS_RETRIEVED, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/{childId}")
	public ResponseEntity<SuccessResponse<List<CouponResponse>>> getCouponsByParent(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long childId
	) {
		List<CouponResponse> response = couponQueryUseCase.getCouponsByParent(currentAuth.memberId(), childId);

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPONS_RETRIEVED, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@PostMapping("/{childId}")
	public ResponseEntity<SuccessResponse<CouponResponse>> createCoupon(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long childId,
		@Valid @RequestBody CouponCreateRequest request
	) {
		CouponResponse response = couponCommandUseCase.createCoupon(currentAuth.memberId(), childId, request);

		return ResponseEntity
			.status(CouponSuccessCode.COUPON_CREATED.getHttpStatus())
			.body(SuccessResponse.of(CouponSuccessCode.COUPON_CREATED, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@PatchMapping("/{couponId}")
	public ResponseEntity<SuccessResponse<CouponResponse>> updateCoupon(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long couponId,
		@Valid @RequestBody CouponUpdateRequest request
	) {
		CouponResponse response = couponCommandUseCase.updateCoupon(currentAuth.memberId(), couponId, request);

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPON_UPDATED, response));
	}

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@DeleteMapping("/{couponId}")
	public ResponseEntity<SuccessResponse<?>> deleteCoupon(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long couponId
	) {
		couponCommandUseCase.deleteCoupon(currentAuth.memberId(), couponId);

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPON_DELETED));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@PostMapping("/{couponId}/purchase")
	public ResponseEntity<SuccessResponse<CouponResponse>> purchaseCoupon(
		@CurrentMember CurrentAuth currentAuth,
		@PathVariable Long couponId
	) {
		CouponResponse dto = couponCommandUseCase.purchaseCoupon(currentAuth.memberId(), couponId);

		CouponResponse response = new CouponResponse(dto.couponId(), dto.name(), dto.price());

		return ResponseEntity.ok(SuccessResponse.of(CouponSuccessCode.COUPON_PURCHASED, response));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@GetMapping("/history")
	public ResponseEntity<SuccessResponse<List<CouponHistoryResponse>>> getCouponHistory(
		@CurrentMember CurrentAuth currentAuth
	) {
		List<CouponHistoryResponse> response = couponQueryUseCase.getCouponHistory(currentAuth.memberId());

		return ResponseEntity.ok()
			.body(SuccessResponse.of(CouponSuccessCode.COUPON_HISTORY_RETRIEVED, response));
	}
}