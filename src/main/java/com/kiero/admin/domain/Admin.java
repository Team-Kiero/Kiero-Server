package com.kiero.admin.domain;

import com.kiero.global.entity.BaseTimeEntity;

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
@Table(name = AdminTableConstants.TABLE_ADMIN)
public class Admin extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = AdminTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = AdminTableConstants.COLUMN_LOGIN_ID, nullable = false, unique = true)
	private String loginId;

	@Column(name = AdminTableConstants.COLUMN_PASSWORD, nullable = false)
	private String password;
}
