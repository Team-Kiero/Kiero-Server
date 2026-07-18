package com.kiero.admin.application.dto;

import java.time.LocalDateTime;

import com.kiero.child.domain.Child;

public record AdminChildSummaryResponse(
	Long id,
	String lastName,
	String firstName,
	int coinAmount,
	LocalDateTime createdAt
) {

	public static AdminChildSummaryResponse from(Child child) {
		return new AdminChildSummaryResponse(
			child.getId(),
			child.getLastName(),
			child.getFirstName(),
			child.getCoinAmount(),
			child.getCreatedAt()
		);
	}
}
