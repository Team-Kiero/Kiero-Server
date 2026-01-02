package com.kiero.global.auth.jwt.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.exception.TokenSuccessCode;
import com.kiero.global.auth.jwt.service.TokenService;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.global.auth.jwt.service.AuthService;
import com.kiero.parent.service.ParentService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/tokens")
public class MemberTokenController {

	private final TokenService tokenService;
	private final AuthService authService;
	private final ParentService parentService;

	@PostMapping("/logout")
	public ResponseEntity<SuccessResponse<Void>> logout(
		@CurrentMember CurrentAuth currentMember
	) {
		if (currentMember.role() == Role.PARENT) {
			// 부모 로그아웃 시 부모 + 연결된 모든 자식의 토큰 삭제
			parentService.logout(currentMember.memberId(), currentMember.role());
		} else {
			// 자식 또는 관리자는 본인 토큰만 삭제
			tokenService.deleteRefreshToken(currentMember.memberId(), currentMember.role());
		}
		return ResponseEntity.ok()
			.body(SuccessResponse.of(TokenSuccessCode.LOGOUT_SUCCESS));
	}

	@PostMapping("/reissue/access-token")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> reissueAccessToken(
		@CookieValue("refreshToken") String refreshToken
	) {
		AccessTokenGenerateResponse response = authService.generateAccessTokenFromRefreshToken(refreshToken);
		return ResponseEntity.ok()
			.body(SuccessResponse.of(TokenSuccessCode.ACCESS_TOKEN_REISSUE_SUCCESS, response));
	}

	@PostMapping("/reissue/tokens")
	public ResponseEntity<SuccessResponse<AccessTokenGenerateResponse>> reissueTokens(
		@CookieValue("refreshToken") String refreshToken
	) {
		String newRefreshToken = authService.reissueRefreshToken(refreshToken);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken)
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.path("/")
			.maxAge(7 * 24 * 60 * 60)
			.build();

		AccessTokenGenerateResponse response = authService.generateAccessTokenFromRefreshToken(newRefreshToken);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(SuccessResponse.of(TokenSuccessCode.TOKENS_REISSUE_SUCCESS, response));
	}

}
