package com.kiero.child.adapter.out.invitation;

import org.springframework.stereotype.Component;

import com.kiero.child.application.port.out.InviteCodeValidatePort;
import com.kiero.invitation.domain.InviteCode;
import com.kiero.invitation.application.service.InviteCodeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InviteCodeServiceAdapter implements InviteCodeValidatePort {
	private final InviteCodeService inviteCodeService;

	@Override
	public InviteCode validateAndDeleteWithLock(String code, String lastName, String firstName) {
		return inviteCodeService.validateAndConsume(code, lastName, firstName);
	}
}