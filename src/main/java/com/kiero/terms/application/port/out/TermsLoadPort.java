package com.kiero.terms.application.port.out;

import java.util.List;
import java.util.Optional;

import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.enums.TermsType;

public interface TermsLoadPort {
	List<Terms> findAllByIsRequiredTrueAndIsActiveTrue();
	List<Terms> findAllByTypeIn(List<TermsType> types);
	Optional<Terms> findById(Long termsId);
}
