package com.kiero.coupon.domain;

import com.kiero.child.domain.Child;
import com.kiero.global.entity.BaseTimeEntity;
import com.kiero.parent.domain.Parent;

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
@Table(name = CouponTableConstants.TABLE_COUPON)
public class Coupon extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = CouponTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = CouponTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = CouponTableConstants.COLUMN_PRICE, nullable = false)
	private int price;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = CouponTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = CouponTableConstants.COLUMN_CHILD_ID, nullable = false)
	private Child child;

	public static Coupon create(String name, int price, Parent parent, Child child) {
		return Coupon.builder()
			.name(name)
			.price(price)
			.parent(parent)
			.child(child)
			.build();
	}

	public void update(String name, int price) {
		this.name = name;
		this.price = price;
	}
}
