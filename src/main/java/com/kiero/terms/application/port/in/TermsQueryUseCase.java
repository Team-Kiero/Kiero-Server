package com.kiero.terms.application.port.in;

import java.util.List;

import com.kiero.terms.application.dto.ExternalLinkResponse;
import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.dto.TermsAgreementStatusResponse;

public interface TermsQueryUseCase {
	List<RequiredTermsResponse> getRequiredTerms();
	TermsAgreementStatusResponse getRequiredTermsAgreementStatus(Long parentId);
	List<ExternalLinkResponse> getParentExternalLinks();
	List<ExternalLinkResponse> getChildExternalLinks();
}
