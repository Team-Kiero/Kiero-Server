package com.kiero.global.auth.jwt.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JwtValidationType {
	VALID_JWT,
	INVALID_JWT_SIGNATURE,
	INVALID_JWT_TOKEN,
	EXPIRED_JWT_TOKEN,
	UNSUPPORTED_JWT_TOKEN,
	EMPTY_JWT
	;

	public boolean isValid() {
		return this == VALID_JWT;
	}

	public boolean isNotValid() {
		return !isValid();
	}
}