package com.kiero.parent.service;

import org.springframework.stereotype.Service;

import com.kiero.global.infrastructure.sse.service.SseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentSseService {

	private final static String EVENT_NAME = "invite";
	private final SseService sseService;

	public void push(Long parentId, Long childId) {
		sseService.push(
			key(parentId),
			EVENT_NAME,
			childId
		);
	}

	public String key(Long parentId) {
		return "invite:" + parentId;
	}
}
