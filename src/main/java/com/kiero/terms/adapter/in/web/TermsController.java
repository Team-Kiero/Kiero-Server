package com.kiero.terms.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.terms.application.dto.RequiredTermsAgreeRequest;
import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.exception.TermsSuccessCode;
import com.kiero.terms.application.port.in.TermsCommandUseCase;
import com.kiero.terms.application.port.in.TermsQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

	private final TermsQueryUseCase termsQueryUseCase;
	private final TermsCommandUseCase termsCommandUseCase;

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/required")
	public ResponseEntity<SuccessResponse<List<RequiredTermsResponse>>> getRequiredTerms() {
		List<RequiredTermsResponse> response = termsQueryUseCase.getRequiredTerms();

		return ResponseEntity.ok()
			.body(SuccessResponse.of(TermsSuccessCode.REQUIRED_TERMS_GET_SUCCESS, response));
	}

	@PreAuthorize("hasRole('PARENT')")
	@PostMapping("/required")
	public ResponseEntity<SuccessResponse<?>> agreeToTerms(
		@CurrentMember CurrentAuth currentAuth,
		@Valid @RequestBody RequiredTermsAgreeRequest request
	) {
		termsCommandUseCase.agreeToTerms(currentAuth.memberId(), request.termsIds());

		return ResponseEntity
			.status(TermsSuccessCode.TERMS_AGREEMENT_CREATED.getHttpStatus())
			.body(SuccessResponse.of(TermsSuccessCode.TERMS_AGREEMENT_CREATED));
	}
}
