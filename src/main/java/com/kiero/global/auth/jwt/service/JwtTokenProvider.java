package com.kiero.global.auth.jwt.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.enums.JwtValidationType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.access-token-expire-time}")
	private long accessTokenExpireTime;

	@Value("${jwt.refresh-token-expire-time}")
	private long refreshTokenExpireTime;

	public static final long TEMPORARY_ACCESS_TOKEN_EXPIRE_TIME = 60 * 5 * 1000L;

	private static final String MEMBER_ID = "member_Id";
	private static final String ROLE_KEY = "role";
	private static final String TOKEN_TYPE = "typ";
	private static final String SCOPE = "scope";

	@PostConstruct
	protected void init() {
		jwtSecret = Base64.getEncoder().encodeToString(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String issueAccessToken(final Authentication authentication) {
		return issueToken(authentication, accessTokenExpireTime);
	}

	public String issueTemporaryAccessToken(final Authentication authentication, final List<String> scope) {
		return issueSubscribeToken(authentication, scope);
	}

	public String issueRefreshToken(final Authentication authentication) {
		return issueToken(authentication, refreshTokenExpireTime);
	}

	public JwtValidationType validateToken(String token) {
		try {
			getBody(token);
			return JwtValidationType.VALID_JWT;
		} catch (MalformedJwtException ex) {
			log.error("Invalid JWT Token: {}", ex.getMessage());
			return JwtValidationType.INVALID_JWT_TOKEN;
		} catch (ExpiredJwtException ex) {
			log.error("Expired JWT Token: {}", ex.getMessage());
			return JwtValidationType.EXPIRED_JWT_TOKEN;
		} catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT Token: {}", ex.getMessage());
			return JwtValidationType.UNSUPPORTED_JWT_TOKEN;
		} catch (IllegalArgumentException ex) {
			log.error("Empty JWT Token: {}", ex.getMessage());
			return JwtValidationType.EMPTY_JWT;
		} catch (SignatureException ex) {
			log.error("Invalid JWT Signature: {}", ex.getMessage());
			return JwtValidationType.INVALID_JWT_SIGNATURE;
		}
	}

	public Long getMemberIdFromJwt(String token) {
		Claims claims = getBody(token);
		return Long.valueOf(claims.get(MEMBER_ID).toString());
	}

	public Role getRoleFromJwt(String token) {
		Claims claims = getBody(token);
		String roleName = claims.get(ROLE_KEY, String.class);

		String enumValue = roleName.replace("ROLE_", "");
		try {
			return Role.valueOf(enumValue.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new AccessDeniedException("Unknown role in JWT: " + enumValue);
		}
	}

	public List<String> getScopesFromJwt(String token) {
		Claims claims = getBody(token);
		Object raw = claims.get(SCOPE);
		if (raw == null) return List.of();

		if (raw instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return List.of(String.valueOf(raw));
	}

	public String getTokenTypeFromJwt(String token) {
		Claims claims = getBody(token);
		Object raw = claims.get(TOKEN_TYPE);
		return raw == null ? null : String.valueOf(raw);
	}

	public LocalDateTime getExpirationDateTime(String token) {
		Claims claims = getBody(token);
		Date expiration = claims.getExpiration();

		return expiration.toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime();
	}

	private String issueToken(final Authentication authentication, final long expiredTime) {
		final Date now = new Date();

		final Claims claims = Jwts.claims()
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + expiredTime));

		claims.put(MEMBER_ID, authentication.getPrincipal());

		String role = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No authorities found"));

		claims.put(ROLE_KEY, role);

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setClaims(claims)
			.signWith(getSigningKey())
			.compact();
	}

	private String issueSubscribeToken(final Authentication authentication, List<String> scope) {
		final Date now = new Date();

		final Claims claims = Jwts.claims()
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + TEMPORARY_ACCESS_TOKEN_EXPIRE_TIME));

		claims.put(MEMBER_ID, authentication.getPrincipal());

		String role = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No authorities found"));

		claims.put(ROLE_KEY, role);
		claims.put(TOKEN_TYPE, "SUBSCRIBE");
		claims.put(SCOPE, scope);

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setClaims(claims)
			.signWith(getSigningKey())
			.compact();
	}

	private Claims getBody(final String token) {
		return Jwts.parserBuilder()
			.setSigningKey(getSigningKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	private SecretKey getSigningKey() {
		String encodedKey = Base64.getEncoder().encodeToString(jwtSecret.getBytes());
		return Keys.hmacShaKeyFor(encodedKey.getBytes());
	}
}