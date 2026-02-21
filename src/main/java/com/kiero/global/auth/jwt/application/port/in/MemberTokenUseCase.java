package com.kiero.global.auth.jwt.application.port.in;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.application.dto.AccessTokenGenerateResponse;

public interface MemberTokenUseCase {

	void logout(Long memberId, Role role);

	AccessTokenGenerateResponse reissueAccessToken(String refreshToken);

	AccessTokenGenerateResponse issueSubscribeToken(String refreshToken);
}