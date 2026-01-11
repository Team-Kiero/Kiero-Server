package com.kiero.parent.presentation;

import com.kiero.global.infrastructure.sse.service.SseService;
import com.kiero.invitation.service.InviteCodeService;
import com.kiero.parent.presentation.dto.ChildInfoResponse;
import com.kiero.parent.presentation.dto.InviteCodeCreateRequest;
import com.kiero.parent.presentation.dto.InviteCodeCreateResponse;
import com.kiero.parent.presentation.dto.InviteStatusResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.parent.exception.ParentSuccessCode;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.parent.service.ParentService;
import com.kiero.parent.service.ParentSseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/parents")
public class ParentController {

	private static final String REFRESH_TOKEN = "refreshToken";
	private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
	private final ParentService parentService;
    private final InviteCodeService inviteCodeService;
	private final SseService sseService;
	private final ParentSseService parentSseService;

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
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

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
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

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @PostMapping("/invite")
    public ResponseEntity<SuccessResponse<InviteCodeCreateResponse>> invite(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody InviteCodeCreateRequest request
    ) {

        String inviteCode = inviteCodeService.createInviteCode(
                currentAuth.memberId(),
                request.childLastName(),
                request.childFirstName()
        );

        InviteCodeCreateResponse response = InviteCodeCreateResponse.of(
                inviteCode,
                request.childLastName(),
                request.childFirstName()
        );

        return ResponseEntity
                .status(ParentSuccessCode.INVITE_CODE_CREATED.getHttpStatus())
                .body(
                        SuccessResponse.of(
                                ParentSuccessCode.INVITE_CODE_CREATED,
                                response
                        )
                );
    }

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @GetMapping("/invite/status")
    public ResponseEntity<SuccessResponse<InviteStatusResponse>> checkInviteStatus(
            @CurrentMember CurrentAuth currentAuth,
            @RequestParam("childLastName") @NotBlank(message = "자녀의 성은 필수입니다.") String childLastName,
            @RequestParam("childFirstName") @NotBlank(message = "자녀의 이름은 필수입니다.") String childFirstName
    ) {
        InviteStatusResponse response = parentService.checkInviteStatus(
                currentAuth.memberId(),
                childLastName,
                childFirstName
        );

        return ResponseEntity.ok()
                .body(SuccessResponse.of(ParentSuccessCode.INVITE_STATUS_CHECKED, response));
    }

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
    @GetMapping("/children")
    public ResponseEntity<SuccessResponse<List<ChildInfoResponse>>> getMyChildren(
            @CurrentMember CurrentAuth currentAuth
    ) {
        List<ChildInfoResponse> children = parentService.getMyChildren(currentAuth.memberId());

        return ResponseEntity.ok()
                .body(SuccessResponse.of(ParentSuccessCode.GET_CHILDREN_SUCCESS, children));
    }

    @PreAuthorize("hasAnyRole('PARENT', 'ADMIN')")
	@GetMapping("/invite/subscribe")
	public SseEmitter subscribe(
		@CurrentMember CurrentAuth currentAuth
	) {
		Long parentId = currentAuth.memberId();
		return sseService.subscribe(parentSseService.key(parentId));
	}
}
