package com.kiero.terms.adapter.out.persistence;

import org.springframework.stereotype.Component;

import java.util.List;

import com.kiero.terms.application.port.out.TermsAgreementDeletePort;
import com.kiero.terms.application.port.out.TermsAgreementLoadPort;
import com.kiero.terms.application.port.out.TermsAgreementPersistencePort;
import com.kiero.terms.domain.TermsAgreement;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermsAgreementPersistenceAdapter implements TermsAgreementLoadPort, TermsAgreementPersistencePort, TermsAgreementDeletePort {

	private final TermsAgreementRepository termsAgreementRepository;

	@Override
	public boolean existsTermsAgreement(Long parentId, Long termsId) {
		return termsAgreementRepository.existsByParentIdAndTermsIdAndWithdrawnAtIsNull(parentId, termsId);
	}

	@Override
	public List<Long> findActiveAgreedTermsIds(Long parentId) {
		return termsAgreementRepository.findActiveAgreedTermsIdsByParentId(parentId);
	}

	@Override
	public void save(TermsAgreement termsAgreement) {
		termsAgreementRepository.save(termsAgreement);
	}

	@Override
	public void deleteAllByParentId(Long parentId) {
		termsAgreementRepository.deleteAllByParentId(parentId);
	}
}
