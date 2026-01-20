package com.kiero.global.infrastructure.sse.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.auth.enums.Role;
import com.kiero.global.infrastructure.sse.service.EventSseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscribe")
public class EventController {

	private final EventSseService eventSseService;

	@PreAuthorize("hasAnyRole('PARENT', 'CHILD', 'ADMIN')")
	@GetMapping(produces = "text/event-stream")
	public SseEmitter subscribe(
		@CurrentMember CurrentAuth currentAuth,
		@RequestHeader("Authorization") String authorization
	) {
		String token = authorization.substring("Bearer ".length());
		Long memberId = currentAuth.memberId();
		Role role = currentAuth.role();

		if (role == Role.CHILD) {
			return eventSseService.subscribeAsChild(memberId, token);
		} else {
			return eventSseService.subscribeAsParent(memberId, token);
		}
	}
}
