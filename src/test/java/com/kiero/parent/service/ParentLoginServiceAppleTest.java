package com.kiero.parent.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kiero.global.auth.client.dto.SocialLoginResponse;
import com.kiero.global.auth.client.enums.Provider;
import com.kiero.global.auth.enums.Role;
import com.kiero.parent.application.dto.ParentLoginResponse;
import com.kiero.parent.application.port.out.AppleAuthPort;
import com.kiero.parent.application.port.out.AppleSocialLoginPort;
import com.kiero.parent.application.port.out.AuthGeneratePort;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.application.port.out.ParentSavePort;
import com.kiero.parent.application.port.out.SocialLoginPort;
import com.kiero.parent.application.service.ParentLoginService;
import com.kiero.parent.domain.Parent;

@ExtendWith(MockitoExtension.class)
class ParentLoginServiceAppleTest {

	private static final String IDENTITY_TOKEN = "test.identity.token";
	private static final String AUTHORIZATION_CODE = "test-authorization-code";
	private static final String APPLE_REFRESH_TOKEN = "apple-refresh-token";
	private static final String SOCIAL_ID = "apple-sub-001";
	private static final String EMAIL = "user@apple.com";

	@Mock
	private SocialLoginPort kakaoSocialLoginPort;

	@Mock
	private AppleSocialLoginPort appleSocialLoginPort;

	@Mock
	private AppleAuthPort appleAuthPort;

	@Mock
	private ParentLoadPort parentLoadPort;

	@Mock
	private ParentSavePort parentSavePort;

	@Mock
	private AuthGeneratePort authGeneratePort;

	@InjectMocks
	private ParentLoginService parentLoginService;

	@Test
	@DisplayName("신규 Apple 유저가 로그인하면 새 Parent가 생성된다")
	void loginWithAppleIdentityToken_newUser_createsParent() {
		// given
		String name = "홍길동";
		SocialLoginResponse socialResponse = SocialLoginResponse.of(SOCIAL_ID, Provider.APPLE, null, EMAIL, null);
		ParentLoginResponse loginResponse = ParentLoginResponse.of(1L,name, EMAIL, null, Role.PARENT, "access", "refresh");

		when(appleSocialLoginPort.loginWithIdentityToken(IDENTITY_TOKEN)).thenReturn(socialResponse);
		when(appleAuthPort.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(APPLE_REFRESH_TOKEN);
		when(parentLoadPort.findParentBySocialIdAndProvider(SOCIAL_ID, Provider.APPLE)).thenReturn(Optional.empty());
		when(parentSavePort.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));
		when(authGeneratePort.generateLoginResponse(any(Parent.class))).thenReturn(loginResponse);

		// when
		ParentLoginResponse response = parentLoginService.loginWithAppleIdentityToken(IDENTITY_TOKEN, AUTHORIZATION_CODE, name);

		// then
		assertThat(response).isEqualTo(loginResponse);
		verify(parentSavePort).save(argThat(parent ->
			parent.getName().equals("홍길동") &&
			parent.getEmail().equals(EMAIL) &&
			parent.getProvider() == Provider.APPLE &&
			parent.getSocialId().equals(SOCIAL_ID)
		));
	}

	@Test
	@DisplayName("name이 null이면 email을 displayName으로 사용한다")
	void loginWithAppleIdentityToken_nullName_usesEmailAsDisplayName() {
		// given
		SocialLoginResponse socialResponse = SocialLoginResponse.of(SOCIAL_ID, Provider.APPLE, null, EMAIL, null);
		ParentLoginResponse loginResponse = ParentLoginResponse.of(1L,EMAIL, EMAIL, null, Role.PARENT, "access", "refresh");

		when(appleSocialLoginPort.loginWithIdentityToken(IDENTITY_TOKEN)).thenReturn(socialResponse);
		when(appleAuthPort.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(APPLE_REFRESH_TOKEN);
		when(parentLoadPort.findParentBySocialIdAndProvider(SOCIAL_ID, Provider.APPLE)).thenReturn(Optional.empty());
		when(parentSavePort.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));
		when(authGeneratePort.generateLoginResponse(any(Parent.class))).thenReturn(loginResponse);

		// when
		parentLoginService.loginWithAppleIdentityToken(IDENTITY_TOKEN, AUTHORIZATION_CODE, null);

		// then
		verify(parentSavePort).save(argThat(parent ->
			parent.getName().equals(EMAIL)
		));
	}

	@Test
	@DisplayName("name이 빈 문자열이면 email을 displayName으로 사용한다")
	void loginWithAppleIdentityToken_blankName_usesEmailAsDisplayName() {
		// given
		SocialLoginResponse socialResponse = SocialLoginResponse.of(SOCIAL_ID, Provider.APPLE, null, EMAIL, null);
		ParentLoginResponse loginResponse = ParentLoginResponse.of(1L,EMAIL, EMAIL, null, Role.PARENT, "access", "refresh");

		when(appleSocialLoginPort.loginWithIdentityToken(IDENTITY_TOKEN)).thenReturn(socialResponse);
		when(appleAuthPort.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(APPLE_REFRESH_TOKEN);
		when(parentLoadPort.findParentBySocialIdAndProvider(SOCIAL_ID, Provider.APPLE)).thenReturn(Optional.empty());
		when(parentSavePort.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));
		when(authGeneratePort.generateLoginResponse(any(Parent.class))).thenReturn(loginResponse);

		// when
		parentLoginService.loginWithAppleIdentityToken(IDENTITY_TOKEN, AUTHORIZATION_CODE, "");

		// then
		verify(parentSavePort).save(argThat(parent ->
			parent.getName().equals(EMAIL)
		));
	}

	@Test
	@DisplayName("기존 Apple 유저가 로그인하면 email이 업데이트되고 새 저장은 하지 않는다")
	void loginWithAppleIdentityToken_existingUser_updatesEmailWithoutSave() {
		// given
		String newEmail = "new@apple.com";
		SocialLoginResponse socialResponse = SocialLoginResponse.of(SOCIAL_ID, Provider.APPLE, null, newEmail, null);
		Parent existingParent = Parent.create("홍길동", "old@apple.com", null, Role.PARENT, Provider.APPLE, SOCIAL_ID);
		ParentLoginResponse loginResponse = ParentLoginResponse.of(1L,"홍길동", newEmail, null, Role.PARENT, "access", "refresh");

		when(appleSocialLoginPort.loginWithIdentityToken(IDENTITY_TOKEN)).thenReturn(socialResponse);
		when(appleAuthPort.exchangeAuthorizationCode(AUTHORIZATION_CODE)).thenReturn(APPLE_REFRESH_TOKEN);
		when(parentLoadPort.findParentBySocialIdAndProvider(SOCIAL_ID, Provider.APPLE)).thenReturn(Optional.of(existingParent));
		when(authGeneratePort.generateLoginResponse(any(Parent.class))).thenReturn(loginResponse);

		// when
		parentLoginService.loginWithAppleIdentityToken(IDENTITY_TOKEN, AUTHORIZATION_CODE, null);

		// then
		assertThat(existingParent.getEmail()).isEqualTo(newEmail);
		verify(parentSavePort, never()).save(any());
	}
}
