package com.kiero.coupon.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kiero.coupon.domain.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
	List<Coupon> findAllByOrderByPriceAsc();
}
