package com.kiero.global.auth.client.apple;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppleEventType {
	EMAIL_ENABLED("email-enabled"),
	EMAIL_DISABLED("email-disabled"),
	CONSENT_REVOKED("consent-revoked"),
	ACCOUNT_DELETE("account-delete"),
	UNKNOWN(null);

	private final String value;

	@JsonCreator
	public static AppleEventType from(String value) {
		if (value == null) {
			return UNKNOWN;
		}
		return Arrays.stream(values())
			.filter(t -> t.value != null && t.value.equals(value))
			.findFirst()
			.orElse(UNKNOWN);
	}
}
