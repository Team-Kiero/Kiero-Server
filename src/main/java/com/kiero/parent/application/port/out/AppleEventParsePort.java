package com.kiero.parent.application.port.out;

import com.kiero.global.auth.client.apple.dto.AppleServerEvent;

public interface AppleEventParsePort {
	AppleServerEvent parse(String eventJwt);
}
