package com.kiero.terms.application.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.dto.TermsAgreementStatusResponse;
import com.kiero.terms.application.port.in.TermsQueryUseCase;
import com.kiero.terms.application.port.out.TermsAgreementLoadPort;
import com.kiero.terms.application.port.out.TermsLoadPort;
import com.kiero.terms.domain.Terms;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService implements TermsQueryUseCase {

	private final TermsLoadPort termsLoadPort;
	private final TermsAgreementLoadPort termsAgreementLoadPort;

	@Override
	public List<RequiredTermsResponse> getRequiredTerms() {
		return termsLoadPort.findAllByIsRequiredTrueAndIsActiveTrue()
			.stream()
			.map(RequiredTermsResponse::from)
			.toList();
	}

	@Override
	public TermsAgreementStatusResponse getRequiredTermsAgreementStatus(Long parentId) {
		List<Long> requiredTermsIds = termsLoadPort.findAllByIsRequiredTrueAndIsActiveTrue()
			.stream()
			.map(Terms::getId)
			.toList();

		List<Long> agreedTermsIds = termsAgreementLoadPort.findActiveAgreedTermsIds(parentId);

		return new TermsAgreementStatusResponse(new HashSet<>(agreedTermsIds).containsAll(requiredTermsIds));
	}
}
