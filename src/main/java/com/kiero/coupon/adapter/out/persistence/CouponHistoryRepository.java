package com.kiero.coupon.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kiero.coupon.domain.CouponHistory;

@Repository
public interface CouponHistoryRepository extends JpaRepository<CouponHistory, Long> {
	List<CouponHistory> findAllByChildIdOrderByCreatedAtDesc(Long childId);

	@Modifying
	@Query("DELETE FROM CouponHistory ch WHERE ch.child.id = :childId")
	void deleteAllByChildId(@Param("childId") Long childId);
}