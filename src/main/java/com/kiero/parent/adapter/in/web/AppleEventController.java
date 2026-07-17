package com.kiero.parent.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.parent.application.port.in.AppleEventUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/apple")
public class AppleEventController {

	private final AppleEventUseCase appleEventUseCase;

	@PostMapping("/events")
	public ResponseEntity<Void> handleEvent(
		@RequestParam("payload") String payload
	) {
		appleEventUseCase.handle(payload);
		return ResponseEntity.ok().build();
	}
}
