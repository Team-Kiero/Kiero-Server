package com.kiero.parent.application.port.in;

import java.util.List;

import com.kiero.parent.application.dto.ChildInfoResponse;
import com.kiero.parent.application.dto.InviteStatusResponse;

public interface ParentQueryUseCase {
	List<ChildInfoResponse> getMyChildren(Long parentId);
	InviteStatusResponse checkInviteStatus(Long parentId, String childLastName, String childFirstName);
	List<Long> getMyChildIds(Long parentId);
}
