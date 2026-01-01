package com.kiero.parent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.client.exception.ClientErrorCode;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.service.AuthService;
import com.kiero.global.auth.jwt.service.TokenService;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.domain.Parent;
import com.kiero.parent.domain.ParentChild;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.parent.service.socialService.KakaoSocialService;
import com.kiero.parent.service.socialService.SocialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

	private final KakaoSocialService kakaoSocialService;
	private final ParentRepository parentRepository;
	private final AuthService authService;
	private final TokenService tokenService;
	private final ParentChildRepository parentChildRepository;

	@Transactional
	public ParentLoginResponse loginWithAuthorizationCode(String authorizationCode, SocialLoginRequest request) {

		SocialService socialService = socialServiceReturner(request.provider());
		SocialLoginResponse response = socialService.login(authorizationCode, request);

		Parent parent = findParentOrCreateParentWithSocialLoginResponse(response);

		return authService.generateLoginResponse(parent);

	}

	@Transactional
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

	@Transactional
	public void logout(Long parentId) {
		// 1. 부모 본인의 토큰 삭제
		tokenService.deleteRefreshToken(parentId, Role.PARENT);

		// 2. 연결된 모든 자식 조회
		List<ParentChild> parentChildren = parentChildRepository.findAllByParent_Id(parentId);

		// 3. 각 자식의 토큰 삭제
		parentChildren.forEach(parentChild -> {
			Long childId = parentChild.getChild().getId();
			try {
				tokenService.deleteRefreshToken(childId, Role.CHILD);
				log.info("Deleted child token for childId: {}", childId);
			} catch (Exception e) {
				// 자식 토큰이 이미 없을 수도 있으므로 예외 무시
				log.warn("Failed to delete child token for childId: {}, reason: {}", childId, e.getMessage());
			}
		});

		log.info("Parent logout completed for parentId: {}, deleted {} child tokens", parentId, parentChildren.size());
	}

}
