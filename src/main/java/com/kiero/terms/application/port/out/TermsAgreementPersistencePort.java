package com.kiero.terms.application.port.out;

import com.kiero.terms.domain.TermsAgreement;

public interface TermsAgreementPersistencePort {
	void save(TermsAgreement termsAgreement);
}
