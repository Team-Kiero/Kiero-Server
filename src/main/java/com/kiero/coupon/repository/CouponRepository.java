package com.kiero.coupon.repository;

import com.kiero.coupon.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findAllByOrderByPriceAsc();
}
