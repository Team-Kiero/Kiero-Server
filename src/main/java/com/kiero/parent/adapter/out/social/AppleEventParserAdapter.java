package com.kiero.parent.adapter.out.social;

import org.springframework.stereotype.Component;

import com.kiero.global.auth.client.apple.AppleEventJwtParser;
import com.kiero.global.auth.client.apple.dto.AppleServerEvent;
import com.kiero.parent.application.port.out.AppleEventParsePort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleEventParserAdapter implements AppleEventParsePort {

	private final AppleEventJwtParser appleEventJwtParser;

	@Override
	public AppleServerEvent parse(String eventJwt) {
		return appleEventJwtParser.parseAndVerify(eventJwt);
	}
}
