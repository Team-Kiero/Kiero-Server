package com.kiero.admin.application.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record AdminPageResponse<T>(
	List<T> content,
	long totalElements,
	int totalPages,
	int currentPage,
	int size
) {

	public static <T> AdminPageResponse<T> from(Page<T> page) {
		return new AdminPageResponse<>(
			page.getContent(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.getNumber(),
			page.getSize()
		);
	}
}
