package com.kiero.parent.presentation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.jwt.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.service.TokenService;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.parent.exception.ParentSuccessCode;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.global.auth.jwt.service.AuthService;
import com.kiero.parent.service.ParentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/parents")
public class ParentController {

	private static final String REFRESH_TOKEN = "refreshToken";
	private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

	private final ParentService parentService;
	private final TokenService tokenService;
	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<SuccessResponse<ParentLoginResponse>> login(
		@RequestParam("authorizationCode") String authorizationCode,
		@RequestBody SocialLoginRequest request
	) {
		ParentLoginResponse response = parentService.loginWithAuthorizationCode(authorizationCode, request);
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, response.refreshToken())
			.maxAge(COOKIE_MAX_AGE)
			.path("/")
			.secure(true)
			.sameSite("None")
			.httpOnly(true)
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(SuccessResponse.of(ParentSuccessCode.LOGIN_SUCCESS, response));
	}

	@PostMapping("/login/access-token")
	public ResponseEntity<SuccessResponse<ParentLoginResponse>> loginWithAccessToken(
		@RequestParam("accessToken") String accessToken
	) {
		ParentLoginResponse response = parentService.loginWithKakaoAccessToken(accessToken);
		ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, response.refreshToken())
			.maxAge(COOKIE_MAX_AGE)
			.path("/")
			.secure(true)
			.sameSite("None")
			.httpOnly(true)
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(SuccessResponse.of(ParentSuccessCode.LOGIN_SUCCESS, response));
	}

	@PostMapping("/logout")
	public ResponseEntity<SuccessResponse<Void>> logout(
		@CurrentMember Long memberId
	) {
		tokenService.deleteRefreshToken(memberId);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ParentSuccessCode.LOGOUT_SUCCESS));
	}

	@PostMapping("/reissue/access-token")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> reissueAccessToken(
		@CookieValue("refreshToken") String refreshToken
	) {
		AccessTokenGenerateResponse response = authService.generateAccessTokenFromRefreshToken(refreshToken);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(ParentSuccessCode.ACCESS_TOKEN_REISSUE_SUCCESS, response));
	}

}
