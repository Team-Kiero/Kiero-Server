package com.kiero.global.auth.client.apple;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.exception.OAuthErrorCode;
import com.kiero.global.exception.KieroException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppleClientSecretGenerator {

	private static final String APPLE_AUTH_URL = "https://appleid.apple.com";
	private static final long CLIENT_SECRET_EXPIRE_SECONDS = 300L;

	@Value("${apple.team-id}")
	private String teamId;

	@Value("${apple.key-id}")
	private String keyId;

	@Value("${apple.bundle-id}")
	private String bundleId;

	@Value("${apple.private-key}")
	private String privateKey;

	public String generate() {
		try {
			PrivateKey key = buildPrivateKey();
			Instant now = Instant.now();

			return Jwts.builder()
				.setHeaderParam("kid", keyId)
				.setIssuer(teamId)
				.setIssuedAt(Date.from(now))
				.setExpiration(Date.from(now.plusSeconds(CLIENT_SECRET_EXPIRE_SECONDS)))
				.setAudience(APPLE_AUTH_URL)
				.setSubject(bundleId)
				.signWith(key, SignatureAlgorithm.ES256)
				.compact();
		} catch (Exception e) {
			log.error("Apple client secret 생성 실패: {}", e.getMessage());
			throw new KieroException(OAuthErrorCode.APPLE_CLIENT_SECRET_GENERATION_FAILED);
		}
	}

	private PrivateKey buildPrivateKey() throws Exception {
		String cleaned = privateKey
			.replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(cleaned);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		return KeyFactory.getInstance("EC").generatePrivate(spec);
	}
}
