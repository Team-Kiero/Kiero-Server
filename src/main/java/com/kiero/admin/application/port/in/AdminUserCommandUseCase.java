package com.kiero.admin.application.port.in;

public interface AdminUserCommandUseCase {
	void deleteParent(Long parentId);
	void deleteChild(Long childId);
}
