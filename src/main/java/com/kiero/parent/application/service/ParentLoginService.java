package com.kiero.parent.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.client.exception.ClientErrorCode;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.dto.ParentLoginResponse;
import com.kiero.parent.application.port.in.ParentLoginUseCase;
import com.kiero.parent.application.port.out.AppleSocialLoginPort;
import com.kiero.parent.application.port.out.AuthGeneratePort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.application.port.out.SocialLoginPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentLoginService implements ParentLoginUseCase {

	private final SocialLoginPort kakaoSocialLoginPort;
	private final AppleSocialLoginPort appleSocialLoginPort;
	private final ParentLoadPort parentLoadPort;
	private final ParentSavePort parentSavePort;
	private final AuthGeneratePort authGeneratePort;

	@Override
	@Transactional
	public ParentLoginResponse loginWithAuthorizationCode(String authorizationCode, SocialLoginRequest request) {

		SocialLoginPort socialLoginPort = socialServiceReturner(request.provider());
		SocialLoginResponse response = socialLoginPort.login(authorizationCode, request);

		Parent parent = findParentOrCreateParentWithSocialLoginResponse(response);

		return authGeneratePort.generateLoginResponse(parent);
	}

	@Override
	@Transactional
	public ParentLoginResponse loginWithKakaoAccessToken(String kakaoAccessToken) {

		SocialLoginResponse response = kakaoSocialLoginPort.loginWithAccessToken(kakaoAccessToken);
		Parent parent = findParentOrCreateParentWithSocialLoginResponse(response);

		return authGeneratePort.generateLoginResponse(parent);
	}

	@Override
	@Transactional
	public ParentLoginResponse loginWithAppleIdentityToken(String identityToken, String name) {
		SocialLoginResponse response = appleSocialLoginPort.loginWithIdentityToken(identityToken);

		Parent parent = parentLoadPort.findParentBySocialIdAndProvider(response.socialId(), response.provider())
			.map(existing -> {
				existing.updateAppleProfile(response.email());
				return existing;
			})
			.orElseGet(() -> {
				String displayName = (name != null && !name.isBlank()) ? name : response.email();
				Parent newParent = Parent.create(
					displayName,
					response.email(),
					null,
					Role.PARENT,
					Provider.APPLE,
					response.socialId()
				);
				return parentSavePort.save(newParent);
			});

		return authGeneratePort.generateLoginResponse(parent);
	}

	private SocialLoginPort socialServiceReturner(Provider provider) {
		return switch (provider) {
			case KAKAO -> kakaoSocialLoginPort;
			default -> throw new KieroException(ClientErrorCode.PROVIDER_NOT_SUPPORTED);
		};
	}

	private Parent findParentOrCreateParentWithSocialLoginResponse(SocialLoginResponse response) {
		return parentLoadPort.findParentBySocialIdAndProvider(response.socialId(), response.provider())
			.map(parent -> {
				parent.updateKakaoProfile(response.name(), response.email(), response.image());
				return parent;
			})
			.orElseGet(() -> saveSocialInfoToParent(response));
	}

	private Parent saveSocialInfoToParent(SocialLoginResponse response) {
		Parent parent = Parent.create(
			response.name(),
			response.email(),
			response.image(),
			Role.PARENT,
			response.provider(),
			response.socialId()
		);
		return parentSavePort.save(parent);
	}
}