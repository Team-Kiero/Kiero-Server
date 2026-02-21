package com.kiero.global.auth.jwt.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.application.exception.TokenSuccessCode;
import com.kiero.global.auth.jwt.application.port.in.MemberTokenUseCase;
import com.kiero.global.response.dto.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tokens")
public class MemberTokenController {

	private final MemberTokenUseCase memberTokenUseCase;

    @PreAuthorize("hasAnyRole('CHILD', 'PARENT', 'ADMIN')")
	@PostMapping("/logout")
	public ResponseEntity<SuccessResponse<Void>> logout(
		@CurrentMember CurrentAuth currentMember
	) {

		memberTokenUseCase.logout(currentMember.memberId(), currentMember.role());

		return ResponseEntity.ok()
			.body(SuccessResponse.of(TokenSuccessCode.LOGOUT_SUCCESS));
	}

	@PostMapping("/reissue/access-token")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> reissueAccessToken(
		@CookieValue("refreshToken") String refreshToken
	) {
		AccessTokenGenerateResponse response = memberTokenUseCase.reissueAccessToken(refreshToken);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(TokenSuccessCode.ACCESS_TOKEN_REISSUE_SUCCESS, response));
	}

	@PostMapping("/reissue/tokens")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> reissueTokens(
		@CookieValue("refreshToken") String refreshToken
	) {

		MemberTokenUseCase.ReissueTokensResult result = memberTokenUseCase.reissueTokens(refreshToken);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", result.newRefreshToken())
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.path("/")
			.maxAge(7 * 24 * 60 * 60)
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(SuccessResponse.of(
				TokenSuccessCode.TOKENS_REISSUE_SUCCESS,
				result.accessTokenResponse()
			));
	}

	@PostMapping("/subscribe-token")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> issueSubscribeToken(
		@CookieValue("refreshToken") String refreshToken
	) {
		AccessTokenGenerateResponse response = memberTokenUseCase.issueSubscribeToken(refreshToken);

		return ResponseEntity.ok()
			.body(SuccessResponse.of(TokenSuccessCode.SUBSCRIBE_TOKEN_ISSUE_SUCCESS, response));
	}
}
