package com.kiero.child.application.port.in;

import com.kiero.child.application.dto.ChildMeResponse;

public interface ChildMeUseCase {
	ChildMeResponse getMyInfo(Long childId);
}