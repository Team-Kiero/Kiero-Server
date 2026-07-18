package com.kiero.parent.application.port.out;

import com.kiero.global.auth.client.dto.SocialLoginResponse;

public interface AppleSocialLoginPort {
	SocialLoginResponse loginWithIdentityToken(String identityToken);
}
