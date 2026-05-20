package com.kiero.terms.application.port.out;

public interface TermsAgreementLoadPort {
	boolean existsTermsAgreement(Long parentId, Long termsId);
}
