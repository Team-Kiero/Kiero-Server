package com.kiero.parent.application.port.out;

public interface AppleAuthPort {
	String exchangeAuthorizationCode(String authorizationCode);
}
