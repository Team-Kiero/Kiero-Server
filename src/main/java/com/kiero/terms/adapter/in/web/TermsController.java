package com.kiero.terms.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.terms.application.dto.RequiredTermsResponse;
import com.kiero.terms.application.exception.TermsSuccessCode;
import com.kiero.terms.application.port.in.TermsQueryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

	private final TermsQueryUseCase termsQueryUseCase;

	@PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/required")
	public ResponseEntity<SuccessResponse<List<RequiredTermsResponse>>> getRequiredTerms() {
		List<RequiredTermsResponse> response = termsQueryUseCase.getRequiredTerms();

		return ResponseEntity.ok()
			.body(SuccessResponse.of(TermsSuccessCode.REQUIRED_TERMS_GET_SUCCESS, response));
	}
}
