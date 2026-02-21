package com.kiero.global.auth.jwt.infrastructure.persistence.redis;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface TokenRepository extends CrudRepository<Token, String> {
	Optional<Token> findByRefreshToken(String refreshToken);
}
