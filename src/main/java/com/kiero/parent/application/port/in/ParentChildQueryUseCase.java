package com.kiero.parent.application.port.in;

import java.util.List;

import com.kiero.parent.application.dto.ChildInfoResponse;
import com.kiero.parent.application.dto.InviteStatusResponse;
import com.kiero.parent.domain.Parent;

public interface ParentChildQueryUseCase {
	List<Parent> findParentsByChildId(Long childId);
	List<ChildInfoResponse> getMyChildren(Long parentId);
	InviteStatusResponse checkInviteStatus(Long parentId, String childLastName, String childFirstName);
	List<Long> getMyChildIds(Long parentId);
}
