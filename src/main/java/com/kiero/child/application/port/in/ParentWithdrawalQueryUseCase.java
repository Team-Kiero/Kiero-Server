package com.kiero.child.application.port.in;

import com.kiero.child.application.dto.ParentWithdrawalStatusResponse;

public interface ParentWithdrawalQueryUseCase {
	ParentWithdrawalStatusResponse getParentWithdrawalStatus(Long childId);
}
