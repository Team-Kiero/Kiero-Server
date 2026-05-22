package com.kiero.terms.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.terms.application.port.out.TermsLoadPort;
import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.enums.TermsType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TermsPersistenceAdapter implements TermsLoadPort {

	private final TermsRepository termsRepository;

	@Override
	public List<Terms> findAllByIsRequiredTrueAndIsActiveTrue() {
		return termsRepository.findAllByIsRequiredTrueAndIsActiveTrue();
	}

	@Override
	public List<Terms> findAllByTypeIn(List<TermsType> types) {
		return termsRepository.findAllByTypeIn(types);
	}

	@Override
	public Optional<Terms> findById(Long termsId) {
		return termsRepository.findById(termsId);
	}
}
