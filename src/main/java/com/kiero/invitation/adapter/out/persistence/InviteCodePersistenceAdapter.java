package com.kiero.invitation.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.invitation.application.port.out.InviteCodeCommandPort;
import com.kiero.invitation.application.port.out.InviteCodeQueryPort;
import com.kiero.invitation.domain.InviteCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InviteCodePersistenceAdapter implements InviteCodeQueryPort, InviteCodeCommandPort {

	private final InviteCodeRepository inviteCodeRepository;

	@Override
	public Optional<InviteCode> findByCode(String code) {
		return inviteCodeRepository.findById(code);
	}

	@Override
	public boolean existsByCode(String code) {
		return inviteCodeRepository.existsById(code);
	}

	@Override
	public InviteCode save(InviteCode inviteCode) {
		return inviteCodeRepository.save(inviteCode);
	}

	@Override
	public void deleteByCode(String code) {
		inviteCodeRepository.deleteById(code);
	}
}
