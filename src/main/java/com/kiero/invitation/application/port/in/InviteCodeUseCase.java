package com.kiero.invitation.application.port.in;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeUseCase {

	String createInviteCode(Long parentId, String childLastName, String childFirstName);

	InviteCode validateAndConsume(String code, String inputLastName, String inputFirstName);
}