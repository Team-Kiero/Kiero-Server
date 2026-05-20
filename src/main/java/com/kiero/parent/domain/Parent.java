package com.kiero.parent.domain;

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
@Table(name = com.kiero.parent.domain.ParentTableConstants.TABLE_PARENT)
public class Parent extends BaseTimeEntity {

	@Id
	@Column(name = ParentTableConstants.COLUMN_ID)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = ParentTableConstants.COLUMN_NAME, nullable = false)
	private String name;

	@Column(name = ParentTableConstants.COLUMN_EMAIL, nullable = false)
	private String email;

	@Column(name = ParentTableConstants.COLUMN_IMAGE, nullable = true)
	private String image;

	@Enumerated(EnumType.STRING)
	@Column(name = ParentTableConstants.COLUMN_ROLE, nullable = false)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(name = ParentTableConstants.COLUMN_PROVIDER, nullable = false)
	private Provider provider;

	@Column(name = ParentTableConstants.COLUMN_SOCIAL_ID, nullable = false)
	private String socialId;

	@Column(name = ParentTableConstants.COLUMN_APPLE_REFRESH_TOKEN)
	private String appleRefreshToken;

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

	public void updateKakaoProfile(final String name, final String email, final String image) {
		this.name = name;
		this.email = email;
		this.image = image;
	}

	public void updateAppleProfile(final String email) {
		this.email = email;
	}

	public void updateAppleRefreshToken(final String appleRefreshToken) {
		this.appleRefreshToken = appleRefreshToken;
	}

}
