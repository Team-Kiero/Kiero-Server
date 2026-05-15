package com.kiero.global.auth.client.apple;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.auth.client.apple.dto.ApplePublicKeyResponse;
import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.exception.KieroException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@ExtendWith(MockitoExtension.class)
class AppleJwtValidatorTest {

	private static final String TEST_BUNDLE_ID = "com.kiero.app";
	private static final String TEST_KID = "test-kid-123";
	private static final String TEST_SUBJECT = "apple-user-sub-001";
	private static final String TEST_EMAIL = "test@example.com";

	@Mock
	private ApplePublicKeyProvider applePublicKeyProvider;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private AppleJwtValidator appleJwtValidator;

	private KeyPair keyPair;

	@BeforeEach
	void setUp() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		keyPair = generator.generateKeyPair();
		ReflectionTestUtils.setField(appleJwtValidator, "bundleId", TEST_BUNDLE_ID);
	}

	@Test
	@DisplayName("유효한 identity token이면 Claims를 반환한다")
	void validateAndExtractClaims_validToken_returnsClaims() throws Exception {
		// given
		String identityToken = createIdentityToken(TEST_KID, APPLE_ISS, TEST_BUNDLE_ID, 3600);
		when(applePublicKeyProvider.getPublicKeys()).thenReturn(createPublicKeyResponse(TEST_KID));

		// when
		var claims = appleJwtValidator.validateAndExtractClaims(identityToken);

		// then
		assertThat(claims.getSubject()).isEqualTo(TEST_SUBJECT);
		assertThat(claims.get("email", String.class)).isEqualTo(TEST_EMAIL);
	}

	@Test
	@DisplayName("캐시에 kid가 없으면 캐시를 갱신하고 재시도한다")
	void validateAndExtractClaims_kidNotInCache_evictsAndRetries() throws Exception {
		// given
		String newKid = "new-kid-456";
		String identityToken = createIdentityToken(newKid, APPLE_ISS, TEST_BUNDLE_ID, 3600);

		when(applePublicKeyProvider.getPublicKeys())
			.thenReturn(createPublicKeyResponse(TEST_KID))  // 첫 번째: 오래된 kid만 존재
			.thenReturn(createPublicKeyResponse(newKid));   // 두 번째: 갱신 후 새 kid 존재

		// when
		var claims = appleJwtValidator.validateAndExtractClaims(identityToken);

		// then
		assertThat(claims.getSubject()).isEqualTo(TEST_SUBJECT);
		verify(applePublicKeyProvider).evictCache();
	}

	@Test
	@DisplayName("캐시 갱신 후에도 kid가 없으면 예외를 던진다")
	void validateAndExtractClaims_kidNotFoundAfterRefresh_throwsException() throws Exception {
		// given
		String identityToken = createIdentityToken("unknown-kid", APPLE_ISS, TEST_BUNDLE_ID, 3600);
		when(applePublicKeyProvider.getPublicKeys()).thenReturn(createPublicKeyResponse(TEST_KID));

		// when & then
		assertThatThrownBy(() -> appleJwtValidator.validateAndExtractClaims(identityToken))
			.isInstanceOf(KieroException.class)
			.extracting(e -> ((KieroException)e).getBaseCode())
			.isEqualTo(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
	}

	@Test
	@DisplayName("issuer가 틀리면 예외를 던진다")
	void validateAndExtractClaims_wrongIssuer_throwsException() throws Exception {
		// given
		String identityToken = createIdentityToken(TEST_KID, "https://evil.com", TEST_BUNDLE_ID, 3600);
		when(applePublicKeyProvider.getPublicKeys()).thenReturn(createPublicKeyResponse(TEST_KID));

		// when & then
		assertThatThrownBy(() -> appleJwtValidator.validateAndExtractClaims(identityToken))
			.isInstanceOf(KieroException.class)
			.extracting(e -> ((KieroException)e).getBaseCode())
			.isEqualTo(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
	}

	@Test
	@DisplayName("audience(bundle-id)가 틀리면 예외를 던진다")
	void validateAndExtractClaims_wrongAudience_throwsException() throws Exception {
		// given
		String identityToken = createIdentityToken(TEST_KID, APPLE_ISS, "com.wrong.app", 3600);
		when(applePublicKeyProvider.getPublicKeys()).thenReturn(createPublicKeyResponse(TEST_KID));

		// when & then
		assertThatThrownBy(() -> appleJwtValidator.validateAndExtractClaims(identityToken))
			.isInstanceOf(KieroException.class)
			.extracting(e -> ((KieroException)e).getBaseCode())
			.isEqualTo(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
	}

	@Test
	@DisplayName("만료된 토큰이면 예외를 던진다")
	void validateAndExtractClaims_expiredToken_throwsException() throws Exception {
		// given
		String identityToken = createIdentityToken(TEST_KID, APPLE_ISS, TEST_BUNDLE_ID, -3600);
		when(applePublicKeyProvider.getPublicKeys()).thenReturn(createPublicKeyResponse(TEST_KID));

		// when & then
		assertThatThrownBy(() -> appleJwtValidator.validateAndExtractClaims(identityToken))
			.isInstanceOf(KieroException.class)
			.extracting(e -> ((KieroException)e).getBaseCode())
			.isEqualTo(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
	}

	private static final String APPLE_ISS = "https://appleid.apple.com";

	private String createIdentityToken(String kid, String issuer, String audience, long expiresInSeconds) {
		return Jwts.builder()
			.setHeaderParam("kid", kid)
			.setIssuer(issuer)
			.setAudience(audience)
			.setSubject(TEST_SUBJECT)
			.claim("email", TEST_EMAIL)
			.setExpiration(new Date(System.currentTimeMillis() + expiresInSeconds * 1000))
			.signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
			.compact();
	}

	private ApplePublicKeyResponse createPublicKeyResponse(String kid) {
		RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
		String n = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(rsaPublicKey.getModulus().toByteArray());
		String e = Base64.getUrlEncoder().withoutPadding()
			.encodeToString(rsaPublicKey.getPublicExponent().toByteArray());
		return new ApplePublicKeyResponse(
			List.of(new ApplePublicKeyResponse.Key("RSA", kid, "sig", "RS256", n, e))
		);
	}
}