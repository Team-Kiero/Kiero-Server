package com.kiero.terms.application.port.out;

public interface TermsAgreementDeletePort {
	void deleteAllByParentId(Long parentId);
}
