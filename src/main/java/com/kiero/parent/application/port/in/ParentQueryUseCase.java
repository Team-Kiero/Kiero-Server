package com.kiero.parent.application.port.in;

import org.springframework.stereotype.Component;

import com.kiero.parent.application.dto.ParentMeResponse;

@Component
public interface ParentQueryUseCase {
	ParentMeResponse getMyInfo(Long parentId);
}
