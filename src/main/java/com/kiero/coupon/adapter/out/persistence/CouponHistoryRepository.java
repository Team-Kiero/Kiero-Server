package com.kiero.coupon.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kiero.coupon.domain.CouponHistory;

@Repository
public interface CouponHistoryRepository extends JpaRepository<CouponHistory, Long> {
	List<CouponHistory> findAllByChildIdOrderByCreatedAtDesc(Long childId);
}