package com.kiero.coupon.domain;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = CouponHistoryTableConstants.TABLE_COUPON_HISTORY)
public class CouponHistory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = CouponHistoryTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = CouponHistoryTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = CouponHistoryTableConstants.COLUMN_PRICE, nullable = false)
	private int price;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = CouponHistoryTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static CouponHistory create(String name, int price, Child child) {
		return CouponHistory.builder()
			.name(name)
			.price(price)
			.child(child)
			.build();
	}
}
