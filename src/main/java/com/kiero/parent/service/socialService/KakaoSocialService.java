package com.kiero.parent.service.socialService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kiero.global.auth.client.dto.SocialLoginRequest;
import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.auth.client.kakao.KakaoApiClient;
import com.kiero.global.auth.client.kakao.KakaoAuthApiClient;
import com.kiero.global.auth.client.kakao.dto.KakaoAccessTokenResponse;
import com.kiero.global.auth.client.kakao.dto.KakaoUserResponse;
import com.kiero.global.exception.KieroException;

import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoSocialService implements SocialService {

	private static final String AUTH_CODE = "authorization_code";

	@Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
	private String redirectUri;

	@Value("${spring.security.oauth2.client.registration.kakao.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
	private String clientSecret;

	private final KakaoApiClient kakaoApiClient;
	private final KakaoAuthApiClient kakaoAuthApiClient;

	@Transactional
	@Override
	public SocialLoginResponse login(
		final String authorizationCode,
		final SocialLoginRequest loginRequest
	) {
		log.info("카카오 로그인을 시도합니다. - Authorization Code: {}, Provider: {}", authorizationCode,
			loginRequest.provider());

		// 환경 변수 로그 확인
		log.info("Kakao clientId: {}", clientId);
		log.info("Kakao redirectUri: {}", redirectUri);

		String accessToken;
		try {
			accessToken = getOAuth2Authentication(authorizationCode);
			log.info("access token을 성공적으로 획득하였습니다.: {}", accessToken);
		} catch (FeignException e) {
			log.error("kakao로부터 access token을 가져오는 데 실패했습니다. Error: {}", e.contentUTF8(), e);
			throw new KieroException(OAuthErrorCode.O_AUTH_TOKEN_ERROR);
		}

		return getLoginDto(loginRequest.provider(), getUserInfo(accessToken));
	}

	public SocialLoginResponse loginWithAccessToken(String kakaoAccessToken) {

		KakaoUserResponse kakaoUserResponse = getUserInfo("Bearer " + kakaoAccessToken);
		return getLoginDto(Provider.KAKAO, kakaoUserResponse);
	}

	private String getOAuth2Authentication(
		final String authorizationCode
	) {
		KakaoAccessTokenResponse response;
		try {
			response = kakaoAuthApiClient.getOAuth2AccessToken(
				AUTH_CODE,
				clientId,
				redirectUri,
				authorizationCode,
				clientSecret
			);
		} catch (FeignException e) {
			throw new KieroException(OAuthErrorCode.O_AUTH_TOKEN_ERROR);
		}
		return "Bearer " + response.accessToken();
	}

	private KakaoUserResponse getUserInfo(
		final String accessToken
	) {
		log.info("access token을 사용해 Kakao API로부터 유저 정보를 불러옵니다.");

		KakaoUserResponse response;
		try {
			response = kakaoApiClient.getUserInformation(accessToken);
			log.info("유저 정보를 성공적으로 획득하였습니다.: ID = {}", response.id());
		} catch (FeignException e) {
			log.error("Kakao API로부터 유저 정보를 찾는 데 실패하였습니다. Error: {}", e.contentUTF8(), e);
			throw new KieroException(OAuthErrorCode.GET_INFO_ERROR);
		}
		return response;
	}

	private SocialLoginResponse getLoginDto(
		final Provider provider,
		final KakaoUserResponse kakaoUserResponse
	) {

		// 카카오 기본이미지일 시 키어로 디폴트 이미지 설정 필요.
		String profileImage = kakaoUserResponse.kakaoAccount().profile().profileImageUrl();
		String image =
			(profileImage == null) ? kakaoUserResponse.kakaoAccount().profile().thumbnailImageUrl() : profileImage;

		return SocialLoginResponse.of(
			String.valueOf(kakaoUserResponse.id()),
			provider,
			kakaoUserResponse.kakaoAccount().profile().nickname(),
			kakaoUserResponse.kakaoAccount().email(),
			image
		);
	}

}

