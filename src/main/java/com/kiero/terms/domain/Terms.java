package com.kiero.terms.domain;

import com.kiero.terms.domain.enums.TermsType;

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
@Table(name = TermsTableConstants.TABLE_TERMS)
public class Terms {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = TermsTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = TermsTableConstants.COLUMN_TITLE, nullable = false)
	private String title;

	@Column(name = TermsTableConstants.COLUMN_IS_REQUIRED, nullable = false)
	private boolean isRequired;

	@Column(name = TermsTableConstants.COLUMN_URL, nullable = false)
	private String url;

	@Enumerated(EnumType.STRING)
	@Column(name = TermsTableConstants.COLUMN_TYPE, nullable = false)
	private TermsType type;

	@Column(name = TermsTableConstants.COLUMN_IS_ACTIVE, nullable = false)
	private boolean isActive;
}
