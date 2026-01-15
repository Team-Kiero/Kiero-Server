package com.kiero.coupon.service;

import com.kiero.child.domain.Child;
import com.kiero.child.exception.ChildErrorCode;
import com.kiero.child.repository.ChildRepository;
import com.kiero.coupon.domain.Coupon;
import com.kiero.coupon.exception.CouponErrorCode;
import com.kiero.coupon.presentation.dto.CouponPurchaseEvent;
import com.kiero.coupon.presentation.dto.CouponResponse;
import com.kiero.coupon.repository.CouponRepository;
import com.kiero.global.exception.KieroException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final ApplicationEventPublisher eventPublisher;

    private final CouponRepository couponRepository;
    private final ChildRepository childRepository;

    private final EntityManager em;
    private final ResourceLoader resourceLoader;

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

        eventPublisher.publishEvent(new CouponPurchaseEvent(
            child.getId(),
            coupon.getName(),
            coupon.getPrice(),
            LocalDateTime.now()
        ));

        log.info("Coupon purchased: childId={}, couponId={}, price={}, remainingCoins={}",
                childId, couponId, coupon.getPrice(), child.getCoinAmount());

        return CouponResponse.from(coupon);
    }

    /*
    솝트 데모데이 때 더미데이터를 넣기 위한 메서드
    */
    @Transactional
    public void insertDummy() {
        String deleteSql = loadSql("sql/coupon_delete_dummy.sql");
        String missionSql = loadSql("sql/coupon_insert_dummy.sql");

        Query deleteQuery = em.createNativeQuery(deleteSql);
        deleteQuery.executeUpdate();

        Query missionQuery = em.createNativeQuery(missionSql);
        missionQuery.executeUpdate();
    }

    private String loadSql(String path) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("더미 SQL 로딩 실패", e);
        }
    }
    /*
     */
}
