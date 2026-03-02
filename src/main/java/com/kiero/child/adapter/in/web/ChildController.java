package com.kiero.child.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.child.application.dto.ChildLoginRequest;
import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.application.dto.ChildMeResponse;
import com.kiero.child.application.exception.ChildSuccessCode;
import com.kiero.child.application.port.in.ChildLoginUseCase;
import com.kiero.child.application.port.in.ChildQueryUseCase;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/children")
public class ChildController {

	private static final String REFRESH_TOKEN = "refreshToken";
	private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
	private final ChildLoginUseCase childLoginUseCase;
	private final ChildQueryUseCase childQueryUseCase;

	@PostMapping("/login")
	public ResponseEntity<SuccessResponse<ChildLoginResponse>> login(
		@Valid @RequestBody ChildLoginRequest request
	) {
		ChildLoginResponse response = childLoginUseCase.login(request);
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, response.refreshToken())
			.maxAge(COOKIE_MAX_AGE)
			.path("/")
			.secure(true)
			.sameSite("None")
			.httpOnly(true)
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(SuccessResponse.of(ChildSuccessCode.LOGIN_SUCCESS, response));
	}

	@PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
	@GetMapping("/me")
	public ResponseEntity<SuccessResponse<ChildMeResponse>> getMyInfo(
		@CurrentMember CurrentAuth currentAuth
	) {
		ChildMeResponse response = childQueryUseCase.getMyInfo(currentAuth.memberId());

		return ResponseEntity.ok()
			.body(SuccessResponse.of(ChildSuccessCode.GET_INFO_SUCCESS, response));
	}
}
