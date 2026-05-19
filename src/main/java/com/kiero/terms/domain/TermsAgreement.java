package com.kiero.terms.domain;

import java.time.LocalDateTime;

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
@Table(name = TermsAgreementTableConstants.TABLE_TERMS_AGREEMENT)
public class TermsAgreement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = TermsAgreementTableConstants.COLUMN_ID)
	private Long id;

	@Column(name = TermsAgreementTableConstants.COLUMN_WITHDRAWN_AT)
	private LocalDateTime withdrawnAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = TermsAgreementTableConstants.COLUMN_PARENT_ID, nullable = false)
	private Parent parent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = TermsAgreementTableConstants.COLUMN_TERMS_ID, nullable = false)
	private Terms terms;

	public static TermsAgreement create(Parent parent, Terms terms) {
		return TermsAgreement.builder()
			.parent(parent)
			.terms(terms)
			.build();
	}

	public void withdraw() {
		this.withdrawnAt = LocalDateTime.now();
	}
}
