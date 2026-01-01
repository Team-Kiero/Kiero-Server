package com.kiero.global.auth.jwt.service;

import org.springframework.stereotype.Service;

import com.kiero.global.auth.enums.Role;
import com.kiero.global.auth.jwt.exception.TokenErrorCode;
import com.kiero.global.auth.redis.Token;
import com.kiero.global.auth.redis.TokenRepository;
import com.kiero.global.auth.redis.util.TokenKeyGenerator;
import com.kiero.global.exception.KieroException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

	private final TokenRepository tokenRepository;

	@Transactional
	public void saveRefreshToken(final Long memberId, final String refreshToken, final Role role) {
		log.info("Saving refresh token for memberId: {}", memberId);
		tokenRepository.save(Token.of(memberId, refreshToken, role));
		log.info("Successfully saved refresh token for memberId: {}", memberId);
	}

	public String findRefreshToken(final Long memberId, final Role role) {
		String key = TokenKeyGenerator.refreshKey(memberId, role);
		return tokenRepository.findById(key)
			.map(Token::getRefreshToken)
			.orElseThrow(()-> new KieroException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND));
	}

	@Transactional
	public void deleteRefreshToken(final Long memberId, final Role role) {
		String key = TokenKeyGenerator.refreshKey(memberId, role);
		Token token = tokenRepository.findById(key)
			.orElseThrow(() -> {
				log.error("No refresh token found in Redis for memberId: {}", memberId);
				return new KieroException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
			});
		tokenRepository.delete(token);
		log.info("Successfully deleted refresh token for memberId: {}", memberId);
	}
}
