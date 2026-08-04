package com.kiero.parent.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kiero.global.exception.KieroException;
import com.kiero.invitation.application.port.out.InviteCodeQueryPort;
import com.kiero.parent.application.dto.ParentMeResponse;
import com.kiero.parent.application.exception.ParentErrorCode;
import com.kiero.parent.application.port.in.ParentQueryUseCase;
import com.kiero.parent.application.port.out.ParentLoadPort;
import com.kiero.parent.domain.Parent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentQueryService implements ParentQueryUseCase {

	private final ParentLoadPort parentLoadPort;
	private final InviteCodeQueryPort inviteCodeQueryPort;

	@Override
	@Transactional(readOnly = true)
	public ParentMeResponse getMyInfo(Long parentId) {
		Parent parent = parentLoadPort.findById(parentId)
			.orElseThrow(() -> new KieroException(ParentErrorCode.PARENT_NOT_FOUND));

		boolean hasPendingChildSession = inviteCodeQueryPort.findByParentKey(parentId.toString()).isPresent();

		return new ParentMeResponse(
			parent.getId(),
			parent.getImage(),
			parent.getName(),
			hasPendingChildSession,
			parent.isPushNotificationEnabled()
		);
	}
}
