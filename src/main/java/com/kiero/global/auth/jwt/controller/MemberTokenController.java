package com.kiero.global.auth.jwt.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.jwt.dto.AccessTokenGenerateResponse;
import com.kiero.global.auth.jwt.service.TokenService;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.parent.exception.ParentSuccessCode;
import com.kiero.global.auth.jwt.service.AuthService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/token")
public class MemberTokenController {

	private final TokenService tokenService;
	private final AuthService authService;

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
