package com.kiero.terms.application.dto;

import com.kiero.terms.domain.Link;
import com.kiero.terms.domain.Terms;

public record ExternalLinkResponse(
	String linkType,
	String link
) {
	public static ExternalLinkResponse fromTerms(Terms terms) {
		return new ExternalLinkResponse(terms.getType().name(), terms.getUrl());
	}

	public static ExternalLinkResponse fromLink(Link link) {
		return new ExternalLinkResponse(link.getType().name(), link.getUrl());
	}
}
