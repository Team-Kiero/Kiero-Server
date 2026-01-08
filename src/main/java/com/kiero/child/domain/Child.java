package com.kiero.child.domain;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = ChildTableConstants.TABLE_CHILD)
public class Child extends BaseTimeEntity {

	@Id
	@Column(name = ChildTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = ChildTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = ChildTableConstants.COLUMN_ROLE, nullable = false)
	private Role role;

	@Column(name = ChildTableConstants.COLUMN_COIN_AMOUNT, nullable = false)
	private int coinAmount;

	public static Child create(
            final String name,
            final Role role
    ) {
		return Child.builder()
			.name(name)
            .role(role)
			.coinAmount(0)
			.build();
	}

    public void addCoin(int amount) {
        this.coinAmount += amount;
    }

    public void deductCoin(int amount) {
        this.coinAmount -= amount;
    }

    public boolean hasEnoughCoin(int amount) {
        return this.coinAmount >= amount;
    }
}
