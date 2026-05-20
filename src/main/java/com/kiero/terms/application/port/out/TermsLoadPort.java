package com.kiero.terms.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.terms.domain.Terms;

public interface TermsLoadPort {
	List<Terms> findAllByIsRequiredTrueAndIsActiveTrue();

	Optional<Terms> findById(Long termsId);
}
