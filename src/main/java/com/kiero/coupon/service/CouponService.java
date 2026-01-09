package com.kiero.coupon.service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.coupon.domain.Coupon;
import com.kiero.coupon.exception.CouponErrorCode;
import com.kiero.coupon.presentation.dto.CouponResponse;
import com.kiero.coupon.repository.CouponRepository;
import com.kiero.global.exception.KieroException;
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
    private final ChildRepository childRepository;

    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findAllByOrderByPriceAsc();

        log.info("Retrieved {} coupons", coupons.size());

        return coupons.stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional
    public CouponResponse purchaseCoupon(Long childId, Long couponId) {
        // 1. 자녀 조회
        Child child = childRepository.findByIdWithLock(childId)
                .orElseThrow(() -> new KieroException(ChildErrorCode.CHILD_NOT_FOUND));

        // 2. 쿠폰 조회
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new KieroException(CouponErrorCode.COUPON_NOT_FOUND));

        // 3. 금화 확인
        if (!child.hasEnoughCoin(coupon.getPrice())) {
            log.warn("Insufficient coins: childId={}, required={}, current={}",
                    childId, coupon.getPrice(), child.getCoinAmount());
            throw new KieroException(CouponErrorCode.INSUFFICIENT_COINS);
        }

        // 4. 금화 차감
        child.deductCoin(coupon.getPrice());

        log.info("Coupon purchased: childId={}, couponId={}, price={}, remainingCoins={}",
                childId, couponId, coupon.getPrice(), child.getCoinAmount());

        return CouponResponse.from(coupon);
    }
}
