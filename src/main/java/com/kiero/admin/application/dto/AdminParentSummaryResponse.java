package com.kiero.admin.application.dto;

import java.time.LocalDateTime;

import com.kiero.parent.domain.Parent;

public record AdminParentSummaryResponse(
	Long id,
	String name,
	String email,
	String image,
	String provider,
	LocalDateTime createdAt
) {

	public static AdminParentSummaryResponse from(Parent parent) {
		return new AdminParentSummaryResponse(
			parent.getId(),
			parent.getName(),
			parent.getEmail(),
			parent.getImage(),
			parent.getProvider().name(),
			parent.getCreatedAt()
		);
	}
}
