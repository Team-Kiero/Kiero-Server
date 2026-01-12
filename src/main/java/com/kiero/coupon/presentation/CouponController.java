package com.kiero.coupon.presentation;

import com.kiero.coupon.exception.CouponSuccessCode;
import com.kiero.coupon.presentation.dto.CouponResponse;
import com.kiero.coupon.service.CouponService;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    @PreAuthorize("hasAnyRole('CHILD', 'PARENT', 'ADMIN')")
    @GetMapping
    public ResponseEntity<SuccessResponse<List<CouponResponse>>> getAllCoupons() {
        List<CouponResponse> coupons = couponService.getAllCoupons();

        return ResponseEntity.ok()
                .body(SuccessResponse.of(CouponSuccessCode.COUPONS_RETRIEVED, coupons));
    }

    @PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
    @PatchMapping("/{couponId}")
    public ResponseEntity<SuccessResponse<CouponResponse>> purchaseCoupon(
            @CurrentMember CurrentAuth currentAuth,
            @PathVariable Long couponId
    ) {
        CouponResponse response = couponService.purchaseCoupon(currentAuth.memberId(), couponId);

        return ResponseEntity.ok()
                .body(SuccessResponse.of(CouponSuccessCode.COUPON_PURCHASED, response));
    }
}
