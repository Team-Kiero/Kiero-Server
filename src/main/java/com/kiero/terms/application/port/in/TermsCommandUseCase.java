package com.kiero.terms.application.port.in;

import java.util.List;

public interface TermsCommandUseCase {
	void agreeToTerms(Long parentId, List<Long> termsIds);
}
