package com.kiero.child.presentation;

import com.kiero.child.exception.ChildSuccessCode;
import com.kiero.child.presentation.dto.ChildLoginResponse;
import com.kiero.child.presentation.dto.ChildMeResponse;
import com.kiero.child.presentation.dto.ChildSignupRequest;
import com.kiero.child.service.ChildService;
import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/children")
public class ChildController {

    private static final String REFRESH_TOKEN = "refreshToken";
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    private final ChildService childService;

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponse<ChildLoginResponse>> signup(
            @Valid @RequestBody ChildSignupRequest request
    ) {
        ChildLoginResponse response = childService.signup(request);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, response.refreshToken())
                .maxAge(COOKIE_MAX_AGE)
                .path("/")
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();

        return ResponseEntity
                .status(ChildSuccessCode.SIGNUP_SUCCESS.getHttpStatus())
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(SuccessResponse.of(ChildSuccessCode.SIGNUP_SUCCESS, response));
    }

    @PreAuthorize("hasAnyRole('CHILD', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<ChildMeResponse>> getMyInfo(
            @CurrentMember CurrentAuth currentAuth
    ) {
        ChildMeResponse response = childService.getMyInfo(currentAuth.memberId());

        return ResponseEntity.ok()
                .body(SuccessResponse.of(ChildSuccessCode.GET_INFO_SUCCESS, response));
    }
}
