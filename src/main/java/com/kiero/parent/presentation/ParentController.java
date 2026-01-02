package com.kiero.parent.presentation;

import com.kiero.invitation.service.InviteCodeService;
import com.kiero.parent.presentation.dto.InviteCodeCreateRequest;
import com.kiero.parent.presentation.dto.InviteCodeCreateResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.parent.exception.ParentSuccessCode;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
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
    private final InviteCodeService inviteCodeService;

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

    @PostMapping("/invite")
    public ResponseEntity<SuccessResponse<InviteCodeCreateResponse>> invite(
            @CurrentMember CurrentAuth currentAuth,
            @RequestBody InviteCodeCreateRequest request
    ) {

        String inviteCode = inviteCodeService.createInviteCode(
                currentAuth.memberId(),
                request.childName()
        );

        InviteCodeCreateResponse response = InviteCodeCreateResponse.of(inviteCode, request.childName());

        return ResponseEntity
                .status(ParentSuccessCode.INVITE_CODE_CREATED.getHttpStatus())
                .body(
                        SuccessResponse.of(
                                ParentSuccessCode.INVITE_CODE_CREATED,
                                response
                        )
                );
    }
}
