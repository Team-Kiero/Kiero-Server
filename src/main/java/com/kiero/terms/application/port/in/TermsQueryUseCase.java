package com.kiero.terms.application.port.in;

import java.util.List;

import com.kiero.terms.application.dto.RequiredTermsResponse;

public interface TermsQueryUseCase {
	List<RequiredTermsResponse> getRequiredTerms();
}
