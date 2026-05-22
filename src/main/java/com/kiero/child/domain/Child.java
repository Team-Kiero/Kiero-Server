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

	@Column(name = ChildTableConstants.COLUMN_LAST_NAME, nullable = false)
	private String lastName;

	@Column(name = ChildTableConstants.COLUMN_FIRST_NAME, nullable = false)
	private String firstName;

	@Enumerated(EnumType.STRING)
	@Column(name = ChildTableConstants.COLUMN_ROLE, nullable = false)
	private Role role;

	@Column(name = ChildTableConstants.COLUMN_COIN_AMOUNT, nullable = false)
	private int coinAmount;

	@Column(name = ChildTableConstants.COLUMN_FCM_TOKEN)
	private String fcmToken;

	@Builder.Default
	@Column(name = ChildTableConstants.COLUMN_PUSH_NOTIFICATION_ENABLED, nullable = false)
	private boolean pushNotificationEnabled = false;

	public static Child create(
		final String lastName,
		final String firstName,
		final Role role
	) {
		return Child.builder()
			.lastName(lastName)
			.firstName(firstName)
			.role(role)
			.coinAmount(0)
			.build();
	}

	public String getFullName() {
		return lastName + firstName;
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

	public void updateFcmToken(final String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public void updatePushNotificationEnabled(final boolean pushNotificationEnabled) {
		this.pushNotificationEnabled = pushNotificationEnabled;
	}
}
