package com.kiero.child.application.port.in;

import com.kiero.child.application.dto.ChildLoginRequest;
import com.kiero.child.application.dto.ChildLoginResponse;

public interface ChildLoginUseCase {
	ChildLoginResponse login(ChildLoginRequest request);
}
