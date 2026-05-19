package com.kiero.terms.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.port.in.TermsQueryUseCase;
import com.kiero.terms.application.port.out.TermsLoadPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService implements TermsQueryUseCase {

	private final TermsLoadPort termsLoadPort;

	@Override
	public List<RequiredTermsResponse> getRequiredTerms() {
		return termsLoadPort.findAllByIsRequiredTrueAndIsActiveTrue()
			.stream()
			.map(RequiredTermsResponse::from)
			.toList();
	}
}
