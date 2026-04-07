package com.kiero.admin.application.port.out;

import java.util.Optional;

import com.kiero.admin.domain.Admin;

public interface AdminLoadPort {
	Optional<Admin> findByLoginId(String loginId);
}
