package com.kiero.child.application.port.in;

import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.application.dto.ChildSignupRequest;

public interface ChildSignupUseCase {
	ChildLoginResponse signup(ChildSignupRequest request);
}
