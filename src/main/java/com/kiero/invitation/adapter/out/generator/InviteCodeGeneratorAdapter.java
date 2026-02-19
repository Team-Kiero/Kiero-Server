package com.kiero.invitation.adapter.out.generator;

import org.springframework.stereotype.Component;

import com.kiero.invitation.application.port.out.InviteCodeGeneratorPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InviteCodeGeneratorAdapter implements InviteCodeGeneratorPort {

	private final InviteCodeGenerator inviteCodeGenerator;

	@Override
	public String generate() {
		return inviteCodeGenerator.generate();
	}
}
