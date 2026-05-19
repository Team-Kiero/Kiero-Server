package com.kiero.terms.application.dto;

import com.kiero.terms.domain.Terms;
import com.kiero.terms.domain.enums.TermsType;

public record RequiredTermsResponse(
	Long termsId,
	TermsType termsType,
	String url
) {
	public static RequiredTermsResponse from(Terms terms) {
		return new RequiredTermsResponse(
			terms.getId(),
			terms.getType(),
			terms.getUrl()
		);
	}
}
