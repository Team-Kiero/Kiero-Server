package com.kiero.terms.application.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record RequiredTermsAgreeRequest(
	@NotEmpty
	List<Long> termsIds
) {
}
