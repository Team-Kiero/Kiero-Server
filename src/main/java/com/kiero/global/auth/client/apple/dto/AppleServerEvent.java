package com.kiero.global.auth.client.apple.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kiero.global.auth.client.apple.AppleEventType;

public record AppleServerEvent(
	AppleEventType type,
	String sub,
	@JsonProperty("event_time") long eventTime
) {
}
