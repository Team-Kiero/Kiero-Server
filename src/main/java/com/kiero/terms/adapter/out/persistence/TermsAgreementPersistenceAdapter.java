package com.kiero.terms.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.kiero.terms.application.port.out.TermsAgreementLoadPort;
import com.kiero.terms.application.port.out.TermsAgreementPersistencePort;
import com.kiero.terms.domain.TermsAgreement;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermsAgreementPersistenceAdapter implements TermsAgreementLoadPort, TermsAgreementPersistencePort {

	private final TermsAgreementRepository termsAgreementRepository;

	@Override
	public boolean existsTermsAgreement(Long parentId, Long termsId) {
		return termsAgreementRepository.existsByParentIdAndTermsIdAndWithdrawnAtIsNull(parentId, termsId);
	}

	@Override
	public void save(TermsAgreement termsAgreement) {
		termsAgreementRepository.save(termsAgreement);
	}
}
