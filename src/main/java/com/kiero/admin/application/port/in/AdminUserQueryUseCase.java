package com.kiero.admin.application.port.in;

import org.springframework.data.domain.Pageable;

import com.kiero.admin.application.dto.AdminChildDetailResponse;
import com.kiero.admin.application.dto.AdminChildSummaryResponse;
import com.kiero.admin.application.dto.AdminPageResponse;
import com.kiero.admin.application.dto.AdminParentDetailResponse;
import com.kiero.admin.application.dto.AdminParentSummaryResponse;

public interface AdminUserQueryUseCase {
	AdminPageResponse<AdminParentSummaryResponse> findAllParents(Pageable pageable);
	AdminParentDetailResponse findParent(Long parentId);
	AdminPageResponse<AdminChildSummaryResponse> findAllChildren(Pageable pageable);
	AdminChildDetailResponse findChild(Long childId);
}
