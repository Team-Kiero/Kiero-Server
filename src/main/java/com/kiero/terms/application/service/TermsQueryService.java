package com.kiero.terms.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.terms.application.dto.ExternalLinkResponse;
import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.dto.TermsAgreementStatusResponse;
import com.kiero.terms.application.port.in.TermsQueryUseCase;
import com.kiero.terms.application.port.out.LinkLoadPort;
import com.kiero.terms.application.port.out.TermsAgreementLoadPort;
import com.kiero.terms.application.port.out.TermsLoadPort;
import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.enums.LinkType;
import com.kiero.terms.domain.enums.TermsType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryService implements TermsQueryUseCase {

	private static final List<TermsType> PARENT_TERMS_TYPES = List.of(
		TermsType.SERVICE_TERMS,
		TermsType.PRIVACY_POLICY,
		TermsType.OPENSOURCE_LICENSE
	);

	private static final List<TermsType> CHILD_TERMS_TYPES = List.of(
		TermsType.SERVICE_TERMS,
		TermsType.PRIVACY_POLICY,
		TermsType.OPENSOURCE_LICENSE
	);

	private static final List<LinkType> PARENT_LINK_TYPES = List.of(
		LinkType.CUSTOMER_SUPPORT
	);

	private final TermsLoadPort termsLoadPort;
	private final TermsAgreementLoadPort termsAgreementLoadPort;
	private final LinkLoadPort linkLoadPort;

	@Override
	public List<RequiredTermsResponse> getRequiredTerms() {
		return termsLoadPort.findAllByIsRequiredTrueAndIsActiveTrue()
			.stream()
			.map(RequiredTermsResponse::from)
			.toList();
	}

	@Override
	public TermsAgreementStatusResponse getRequiredTermsAgreementStatus(Long parentId) {
		List<Long> requiredTermsIds = termsLoadPort.findAllByIsRequiredTrueAndIsActiveTrue()
			.stream()
			.map(Terms::getId)
			.toList();

		List<Long> agreedTermsIds = termsAgreementLoadPort.findActiveAgreedTermsIds(parentId);

		return new TermsAgreementStatusResponse(new HashSet<>(agreedTermsIds).containsAll(requiredTermsIds));
	}

	@Override
	public List<ExternalLinkResponse> getParentExternalLinks() {
		List<ExternalLinkResponse> termsLinks = termsLoadPort.findAllByTypeIn(PARENT_TERMS_TYPES)
			.stream()
			.map(ExternalLinkResponse::fromTerms)
			.toList();

		List<ExternalLinkResponse> links = linkLoadPort.findAll()
			.stream()
			.filter(link -> PARENT_LINK_TYPES.contains(link.getType()))
			.map(ExternalLinkResponse::fromLink)
			.toList();

		return Stream.concat(termsLinks.stream(), links.stream()).toList();
	}

	@Override
	public List<ExternalLinkResponse> getChildExternalLinks() {
		return termsLoadPort.findAllByTypeIn(CHILD_TERMS_TYPES)
			.stream()
			.map(ExternalLinkResponse::fromTerms)
			.toList();
	}
}
