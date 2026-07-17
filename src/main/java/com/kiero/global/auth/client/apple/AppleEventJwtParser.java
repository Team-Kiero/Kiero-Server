package com.kiero.global.auth.client.apple;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiero.global.auth.client.apple.dto.ApplePublicKeyResponse;
import com.kiero.global.auth.client.apple.dto.AppleServerEvent;
import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.exception.KieroException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleEventJwtParser {

	private static final String APPLE_ISS = "https://appleid.apple.com";

	@Value("${apple.team-id}")
	private String teamId;

	private final ApplePublicKeyProvider applePublicKeyProvider;
	private final ObjectMapper objectMapper;

	public AppleServerEvent parseAndVerify(String eventJwt) {
		try {
			String kid = extractKid(eventJwt);
			ApplePublicKeyResponse.Key matchingKey = findMatchingKey(kid);
			PublicKey publicKey = buildPublicKey(matchingKey);

			Claims claims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(eventJwt)
				.getBody();

			validateClaims(claims);

			String eventsJson = claims.get("events", String.class);
			return objectMapper.readValue(eventsJson, AppleServerEvent.class);

		} catch (KieroException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apple event token 검증 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
		}
	}

	private ApplePublicKeyResponse.Key findMatchingKey(String kid) {
		Optional<ApplePublicKeyResponse.Key> key = applePublicKeyProvider.getPublicKeys().keys().stream()
			.filter(k -> k.kid().equals(kid))
			.findFirst();

		if (key.isPresent()) {
			return key.get();
		}

		log.info("캐시된 Apple public key에서 kid를 찾지 못했습니다. 캐시를 갱신합니다. kid: {}", kid);
		applePublicKeyProvider.evictCache();

		return applePublicKeyProvider.getPublicKeys().keys().stream()
			.filter(k -> k.kid().equals(kid))
			.findFirst()
			.orElseThrow(() -> {
				log.error("일치하는 Apple public key를 찾을 수 없습니다. kid: {}", kid);
				return new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
			});
	}

	@SuppressWarnings("unchecked")
	private String extractKid(String jwtToken) {
		try {
			String[] parts = jwtToken.split("\\.");
			if (parts.length < 2) {
				throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
			}
			byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
			Map<String, String> header = objectMapper.readValue(headerBytes, Map.class);
			String kid = header.get("kid");
			if (kid == null || kid.isBlank()) {
				throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
			}
			return kid;
		} catch (KieroException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apple event token 헤더 파싱 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
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
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
		}
	}

	private void validateClaims(Claims claims) {
		if (!APPLE_ISS.equals(claims.getIssuer())) {
			log.error("Apple event token issuer 불일치: {}", claims.getIssuer());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
		}
		// identityToken은 aud=bundle-id이지만, event token은 aud=team-id
		if (!teamId.equals(claims.getAudience())) {
			log.error("Apple event token audience 불일치: {}", claims.getAudience());
			throw new KieroException(OAuthErrorCode.INVALID_APPLE_EVENT_TOKEN);
		}
	}
}
