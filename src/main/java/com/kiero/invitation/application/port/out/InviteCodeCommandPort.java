package com.kiero.invitation.application.port.out;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeCommandPort {

	InviteCode save(InviteCode inviteCode);

	void deleteByCode(String code);
}
