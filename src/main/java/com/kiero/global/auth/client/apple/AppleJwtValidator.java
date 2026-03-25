package com.kiero.global.auth.client.apple;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.auth.client.apple.dto.ApplePublicKeyResponse;
import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.exception.KieroException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleJwtValidator {

	private static final String APPLE_ISS = "https://appleid.apple.com";

	@Value("${apple.bundle-id}")
	private String bundleId;

	private final ApplePublicKeyClient applePublicKeyClient;
	private final ObjectMapper objectMapper;

	public Claims validateAndExtractClaims(String identityToken) {
		String kid = extractKid(identityToken);

		ApplePublicKeyResponse publicKeyResponse = applePublicKeyClient.getApplePublicKeys();

		ApplePublicKeyResponse.Key matchingKey = publicKeyResponse.keys().stream()
			.filter(key -> key.kid().equals(kid))
			.findFirst()
			.orElseThrow(() -> {
				log.error("일치하는 Apple public key를 찾을 수 없습니다. kid: {}", kid);
				return new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
			});

		PublicKey publicKey = buildPublicKey(matchingKey);

		try {
			Claims claims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(identityToken)
				.getBody();

			validateClaims(claims);
			return claims;

		} catch (KieroException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apple identity token 검증 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
		}
	}

	@SuppressWarnings("unchecked")
	private String extractKid(String identityToken) {
		try {
			String[] parts = identityToken.split("\\.");
			byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
			Map<String, String> header = objectMapper.readValue(headerBytes, Map.class);
			return header.get("kid");
		} catch (Exception e) {
			log.error("identityToken 헤더 파싱 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
		}
	}

	private PublicKey buildPublicKey(ApplePublicKeyResponse.Key key) {
		try {
			byte[] nBytes = Base64.getUrlDecoder().decode(key.n());
			byte[] eBytes = Base64.getUrlDecoder().decode(key.e());
			RSAPublicKeySpec spec = new RSAPublicKeySpec(
				new BigInteger(1, nBytes),
				new BigInteger(1, eBytes)
			);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		} catch (Exception e) {
			log.error("Apple public key 생성 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
		}
	}

	private void validateClaims(Claims claims) {
		if (!APPLE_ISS.equals(claims.getIssuer())) {
			log.error("Apple identity token issuer 불일치: {}", claims.getIssuer());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
		}
		if (!bundleId.equals(claims.getAudience())) {
			log.error("Apple identity token audience 불일치: {}", claims.getAudience());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_ID_TOKEN);
		}
	}
}
