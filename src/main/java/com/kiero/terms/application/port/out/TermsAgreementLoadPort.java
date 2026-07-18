package com.kiero.terms.application.port.out;

import java.util.List;

public interface TermsAgreementLoadPort {
	boolean existsTermsAgreement(Long parentId, Long termsId);
	List<Long> findActiveAgreedTermsIds(Long parentId);
}
