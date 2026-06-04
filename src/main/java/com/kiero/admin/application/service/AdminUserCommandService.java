package com.kiero.admin.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.admin.application.exception.AdminErrorCode;
import com.kiero.admin.application.port.in.AdminUserCommandUseCase;
import com.kiero.admin.application.port.out.AdminChildLoadPort;
import com.kiero.admin.application.port.out.AdminDeletePort;
import com.kiero.admin.application.port.out.AdminParentLoadPort;
import com.kiero.global.exception.KieroException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserCommandService implements AdminUserCommandUseCase {

	private final AdminParentLoadPort adminParentLoadPort;
	private final AdminChildLoadPort adminChildLoadPort;
	private final AdminDeletePort adminDeletePort;

	@Override
	@Transactional
	public void deleteParent(Long parentId) {
		adminParentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.PARENT_NOT_FOUND));

		adminDeletePort.deleteAllSchedulesByParentId(parentId);
		adminDeletePort.deleteAllMissionsByParentId(parentId);
		adminDeletePort.deleteAllCouponsByParentId(parentId);
		adminDeletePort.deleteAllFeedItemsByParentId(parentId);
		adminDeletePort.deleteAllParentChildRelationsByParentId(parentId);
		adminDeletePort.deleteParent(parentId);
	}

	@Override
	@Transactional
	public void deleteChild(Long childId) {
		adminChildLoadPort.findById(childId)
			.orElseThrow(() -> new KieroException(AdminErrorCode.CHILD_NOT_FOUND));

		adminDeletePort.deleteAllSchedulesByChildId(childId);
		adminDeletePort.deleteAllMissionsByChildId(childId);
		adminDeletePort.deleteAllCouponsByChildId(childId);
		adminDeletePort.deleteAllCouponHistoriesByChildId(childId);
		adminDeletePort.deleteAllFeedItemsByChildId(childId);
		adminDeletePort.deleteAllParentChildRelationsByChildId(childId);
		adminDeletePort.deleteChild(childId);
	}
}
