package com.kiero.admin.application.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.dto.AdminChildDetailResponse;
import com.kiero.admin.application.dto.AdminChildSummaryResponse;
import com.kiero.admin.application.dto.AdminPageResponse;
import com.kiero.admin.application.dto.AdminParentDetailResponse;
import com.kiero.admin.application.dto.AdminParentSummaryResponse;
import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminUserQueryUseCase;
import com.kiero.admin.application.port.out.AdminChildLoadPort;
import com.kiero.admin.application.port.out.AdminParentLoadPort;
import com.kiero.child.domain.Child;
import com.kiero.global.exception.KieroException;
import com.kiero.parent.application.port.out.ParentChildLoadPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserQueryService implements AdminUserQueryUseCase {

	private final AdminParentLoadPort adminParentLoadPort;
	private final AdminChildLoadPort adminChildLoadPort;
	private final ParentChildLoadPort parentChildLoadPort;

	@Override
	@Transactional(readOnly = true)
	public AdminPageResponse<AdminParentSummaryResponse> findAllParents(Pageable pageable) {
		return AdminPageResponse.from(
			adminParentLoadPort.findAll(pageable).map(AdminParentSummaryResponse::from)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminParentDetailResponse findParent(Long parentId) {
		Parent parent = adminParentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.PARENT_NOT_FOUND));

		List<AdminChildSummaryResponse> children = parentChildLoadPort.findAllByParentId(parentId)
			.stream()
			.map(pc -> AdminChildSummaryResponse.from(pc.getChild()))
			.toList();

		return AdminParentDetailResponse.of(parent, children);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminPageResponse<AdminChildSummaryResponse> findAllChildren(Pageable pageable) {
		return AdminPageResponse.from(
			adminChildLoadPort.findAll(pageable).map(AdminChildSummaryResponse::from)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminChildDetailResponse findChild(Long childId) {
		Child child = adminChildLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.CHILD_NOT_FOUND));

		List<AdminParentSummaryResponse> parents = parentChildLoadPort.findParentsByChildId(childId)
			.stream()
			.map(AdminParentSummaryResponse::from)
			.toList();

		return AdminChildDetailResponse.of(child, parents);
	}
}
