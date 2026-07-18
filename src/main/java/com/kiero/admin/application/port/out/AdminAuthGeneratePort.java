package com.kiero.admin.application.port.out;

import com.kiero.admin.application.dto.AdminLoginResponse;
import com.kiero.admin.domain.Admin;

public interface AdminAuthGeneratePort {
	AdminLoginResponse generateLoginResponse(Admin admin);
}
