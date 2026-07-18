package com.kiero.admin.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.child.domain.Child;

public record AdminChildDetailResponse(
	Long id,
	String lastName,
	String firstName,
	int coinAmount,
	LocalDateTime createdAt,
	List<AdminParentSummaryResponse> parents
) {

	public static AdminChildDetailResponse of(Child child, List<AdminParentSummaryResponse> parents) {
		return new AdminChildDetailResponse(
			child.getId(),
			child.getLastName(),
			child.getFirstName(),
			child.getCoinAmount(),
			child.getCreatedAt(),
			parents
		);
	}
}
