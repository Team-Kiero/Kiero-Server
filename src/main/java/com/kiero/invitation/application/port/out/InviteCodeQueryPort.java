package com.kiero.invitation.application.port.out;

import java.util.Optional;

import com.kiero.invitation.domain.InviteCode;

public interface InviteCodeQueryPort {
	Optional<InviteCode> findByCode(String code);

	boolean existsByCode(String code);
}
