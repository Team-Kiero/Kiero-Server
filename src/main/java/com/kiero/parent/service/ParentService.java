package com.kiero.parent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.client.exception.ClientErrorCode;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.parent.service.socialService.KakaoSocialService;
import com.kiero.parent.service.socialService.SocialService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentService {

	private final KakaoSocialService kakaoSocialService;
	private final ParentRepository parentRepository;
	private final AuthService authService;

	@Transactional
	public ParentLoginResponse loginWithAuthorizationCode(String authorizationCode, SocialLoginRequest request) {

		SocialService socialService = socialServiceReturner(request.provider());
		SocialLoginResponse response = socialService.login(authorizationCode, request);

		Parent parent = findParentOrCreateParentWithSocialLoginResponse(response);

		return authService.generateLoginResponse(parent);

	}

	public ParentLoginResponse loginWithKakaoAccessToken(String kakaoAccessToken) {

		SocialLoginResponse response = kakaoSocialService.loginWithAccessToken(kakaoAccessToken);
		Parent parent = findParentOrCreateParentWithSocialLoginResponse(response);

		return authService.generateLoginResponse(parent);
	}

	public Parent findParentOrCreateParentWithSocialLoginResponse(SocialLoginResponse response){
		return parentRepository.findParentBySocialIdAndProvider(response.socialId(), response.provider())
			.orElseGet(()-> saveSocialInfoToParent(response));
	}

	private SocialService socialServiceReturner(Provider provider) {
		return switch (provider) {
			case KAKAO -> kakaoSocialService;
			default -> throw new KieroException(ClientErrorCode.PROVIDER_NOT_SUPPORTED);
		};
	}

	private Parent saveSocialInfoToParent(SocialLoginResponse response) {
		Parent parent = Parent.create(response.name(), response.email(), response.image(), Role.PARENT, response.provider(), response.socialId());
		return parentRepository.save(parent);
	}

}
