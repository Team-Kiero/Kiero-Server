package com.kiero.admin.application.port.in;

import com.kiero.admin.application.dto.AdminLoginRequest;
import com.kiero.admin.application.dto.AdminLoginResponse;

public interface AdminLoginUseCase {
	AdminLoginResponse login(AdminLoginRequest request);
}
