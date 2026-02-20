package com.kiero.global.sse.application.port.out;

import java.time.LocalDateTime;

public interface TokenExpiryPort {
	LocalDateTime getExpirationDateTime(String token);

}
