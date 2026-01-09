package com.kiero.schedule.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record NowScheduleCompleteRequest(
	@NotNull
	String imageUrl
) {
}
