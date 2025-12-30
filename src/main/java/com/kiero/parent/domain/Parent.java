package com.kiero.parent.domain;

import java.time.LocalDateTime;

import com.kiero.global.auth.client.enums.Provider;
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
@Table(name = ParentTableConstants.TABLE_PARENT)
public class Parent extends BaseTimeEntity {

	@Id
	@Column(name = ParentTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = ParentTableConstants.COLUMN_NAME)
	private String name;

	@Column(name = ParentTableConstants.COLUMN_EMAIL)
	private String email;

	@Column(name = ParentTableConstants.COLUMN_IMAGE)
	private String image;

	@Enumerated(EnumType.STRING)
	@Column(name = ParentTableConstants.COLUMN_ROLE)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = ParentTableConstants.COLUMN_PROVIDER)
	private Provider provider;

	@Column(name = ParentTableConstants.COLUMN_SOCIAL_ID)
	private String socialId;

	public static Parent create(
		final String name,
		final String email,
		final String image,
		final Role role,
		final Provider provider,
		final String socialId
	) {
		return Parent.builder()
			.name(name)
			.email(email)
			.image(image)
			.role(role)
			.provider(provider)
			.socialId(socialId)
			.build();
	}

}
