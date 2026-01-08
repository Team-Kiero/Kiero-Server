package com.kiero.coupon.service;

import com.kiero.coupon.domain.Coupon;
import com.kiero.coupon.presentation.dto.CouponResponse;
import com.kiero.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findAllByOrderByPriceAsc();

        log.info("Retrieved {} coupons", coupons.size());

        return coupons.stream()
                .map(CouponResponse::from)
                .toList();
    }
}
