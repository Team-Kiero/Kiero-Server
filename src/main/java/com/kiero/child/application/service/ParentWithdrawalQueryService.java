package com.kiero.child.application.service;

import org.springframework.stereotype.Service;

import com.kiero.child.application.dto.ParentWithdrawalStatusResponse;
import com.kiero.child.application.port.in.ParentWithdrawalQueryUseCase;
import com.kiero.child.application.port.out.ParentWithdrawCheckPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentWithdrawalQueryService implements ParentWithdrawalQueryUseCase {

	private final ParentWithdrawCheckPort parentWithdrawCheckPort;

	@Override
	public ParentWithdrawalStatusResponse getParentWithdrawalStatus(Long childId) {
		return new ParentWithdrawalStatusResponse(parentWithdrawCheckPort.isParentWithdrawn(childId));
	}
}
