package com.kiero.parent.application.port.out;

import com.kiero.parent.application.dto.ParentLoginResponse;
import com.kiero.parent.domain.Parent;

public interface AuthGeneratePort {
	ParentLoginResponse generateLoginResponse(Parent parent);
}
