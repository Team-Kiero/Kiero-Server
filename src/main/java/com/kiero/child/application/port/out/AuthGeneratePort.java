package com.kiero.child.application.port.out;

import com.kiero.child.application.dto.ChildLoginResponse;
import com.kiero.child.domain.Child;

public interface AuthGeneratePort {
	ChildLoginResponse generateLoginResponse(Child child);
}
