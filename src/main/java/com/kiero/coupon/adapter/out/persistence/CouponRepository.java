package com.kiero.coupon.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kiero.coupon.domain.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
	List<Coupon> findAllByChildIdOrderByPriceAscUpdatedAtDesc(Long childId);

	List<Coupon> findAllByChildId(Long childId);

	@Modifying
	@Query("DELETE FROM Coupon c WHERE c.child.id = :childId")
	void deleteAllByChildId(@Param("childId") Long childId);

	@Modifying
	@Query("DELETE FROM Coupon c WHERE c.parent.id = :parentId")
	void deleteAllByParentId(@Param("parentId") Long parentId);
}
