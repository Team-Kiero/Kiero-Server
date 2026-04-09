package com.kiero.admin.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kiero.admin.application.port.out.AdminLoadPort;
import com.kiero.admin.domain.Admin;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminPersistenceAdapter implements AdminLoadPort {

	private final AdminRepository adminRepository;

	@Override
	public Optional<Admin> findByLoginId(String loginId) {
		return adminRepository.findByLoginId(loginId);
	}
}
