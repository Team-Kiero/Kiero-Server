package com.kiero.parent.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.child.domain.Child;
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
import com.kiero.parent.presentation.dto.ChildInfoResponse;
import com.kiero.parent.presentation.dto.InviteStatusResponse;
import com.kiero.parent.presentation.dto.ParentLoginResponse;
import com.kiero.parent.repository.ParentChildRepository;
import com.kiero.parent.repository.ParentRepository;
import com.kiero.parent.service.socialService.KakaoSocialService;
import com.kiero.parent.service.socialService.SocialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
	public void logout(Long parentId, Role role) {
		// 1. 부모 본인의 토큰 삭제
		tokenService.deleteRefreshToken(parentId, role);

		// 2. 연결된 모든 아이ID 조회
		List<Long> childIds = parentChildRepository.findChildIdsByParentId(parentId);

		// 3. 모든 아이의 토큰을 한 번에 벌크 삭제
		if (!childIds.isEmpty()) {
			try {
				tokenService.deleteRefreshTokensBulk(childIds, Role.CHILD);
				log.info("Parent logout completed for parentId: {}, deleted {} child tokens",
						parentId, childIds.size());
			} catch (Exception e) {
				log.warn("Failed to bulk delete child tokens for parentId: {}, reason: {}",
						parentId, e.getMessage());
			}
		} else {
			log.info("Parent logout completed for parentId: {}, no child tokens to delete", parentId);
		}
	}

	@Transactional(readOnly = true)
	public List<ChildInfoResponse> getMyChildren(Long parentId) {
		List<ParentChild> parentChildren = parentChildRepository.findAllByParentId(parentId);

		return parentChildren.stream()
				.map(pc -> ChildInfoResponse.of(pc.getChild()))
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public InviteStatusResponse checkInviteStatus(Long parentId, String childLastName, String childFirstName) {
		List<ParentChild> parentChildren = parentChildRepository.findAllByParentId(parentId);

		Optional<Child> matchedChild = parentChildren.stream()
				.map(ParentChild::getChild)
				.filter(child -> child.getLastName().trim().equals(childLastName.trim()) &&
								 child.getFirstName().trim().equals(childFirstName.trim()))
				.findFirst();

		if (matchedChild.isPresent()) {
			Child child = matchedChild.get();
			log.info("Child found: parentId={}, childId={}, name={} {}",
					parentId, child.getId(), childLastName, childFirstName);
			return InviteStatusResponse.registered(child.getId());
		} else {
			log.info("Child not found: parentId={}, name={} {}",
					parentId, childLastName, childFirstName);
			return InviteStatusResponse.notRegistered();
		}
	}

}
