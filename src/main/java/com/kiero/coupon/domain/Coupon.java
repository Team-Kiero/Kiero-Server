package com.kiero.coupon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = CouponTableConstants.TABLE_COUPON)
public class Coupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = CouponTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = CouponTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = CouponTableConstants.COLUMN_PRICE, nullable = false)
	private int price;

	public static Coupon create(String name, int price) {
		return Coupon.builder()
			.name(name)
			.price(price)
			.build();
	}
}