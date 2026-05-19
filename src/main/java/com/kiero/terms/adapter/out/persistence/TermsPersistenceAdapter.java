package com.kiero.terms.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kiero.terms.application.port.out.TermsLoadPort;
import com.kiero.terms.domain.Terms;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermsPersistenceAdapter implements TermsLoadPort {

	private final TermsRepository termsRepository;

	@Override
	public List<Terms> findAllByIsRequiredTrueAndIsActiveTrue() {
		return termsRepository.findAllByIsRequiredTrueAndIsActiveTrue();
	}
}
