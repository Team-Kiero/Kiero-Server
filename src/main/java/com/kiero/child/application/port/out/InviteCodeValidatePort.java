package com.kiero.child.application.port.out;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeValidatePort {
	InviteCode validateAndDeleteWithLock(String code, String lastName, String firstName);
}
