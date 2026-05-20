package com.kiero.terms.application.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;
import com.kiero.terms.application.exception.TermsErrorCode;
import com.kiero.terms.application.port.in.TermsCommandUseCase;
import com.kiero.terms.application.port.out.TermsAgreementPersistencePort;
import com.kiero.terms.application.port.out.TermsLoadPort;
import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.TermsAgreement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TermsCommandService implements TermsCommandUseCase {

	private final ParentLoadPort parentLoadPort;
	private final TermsLoadPort termsLoadPort;
	private final TermsAgreementPersistencePort termsAgreementPersistencePort;

	@Override
	public void agreeToTerms(Long parentId, List<Long> termsIds) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(TermsErrorCode.PARENT_NOT_FOUND));

		if (termsIds.size() != new HashSet<>(termsIds).size()) {
			throw new KieroException(TermsErrorCode.DUPLICATE_TERMS_ID);
		}

		for (Long termsId : termsIds) {
			Terms terms = termsLoadPort.findById(termsId)
				.orElseThrow(() -> new KieroException(TermsErrorCode.TERMS_NOT_FOUND));

			termsAgreementPersistencePort.save(TermsAgreement.create(parent, terms));
		}
	}
}
