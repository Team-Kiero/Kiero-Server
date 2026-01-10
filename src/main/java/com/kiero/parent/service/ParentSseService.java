package com.kiero.parent.service;

import org.springframework.stereotype.Service;

import com.kiero.global.infrastructure.sse.service.SseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentSseService {
	private final SseService sseService;

	public void push(Long parentId, Long childId) {
		sseService.push(
			key(parentId),
			"invite",
			childId
		);
	}

	public String key(Long parentId) {
		return "invite:" + parentId;
	}
}
