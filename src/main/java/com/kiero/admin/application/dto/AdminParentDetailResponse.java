package com.kiero.admin.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.kiero.parent.domain.Parent;

public record AdminParentDetailResponse(
	Long id,
	String name,
	String email,
	String image,
	String provider,
	LocalDateTime createdAt,
	List<AdminChildSummaryResponse> children
) {

	public static AdminParentDetailResponse of(Parent parent, List<AdminChildSummaryResponse> children) {
		return new AdminParentDetailResponse(
			parent.getId(),
			parent.getName(),
			parent.getEmail(),
			parent.getImage(),
			parent.getProvider().name(),
			parent.getCreatedAt(),
			children
		);
	}
}
