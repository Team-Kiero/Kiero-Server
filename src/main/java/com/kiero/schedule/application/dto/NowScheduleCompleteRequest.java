package com.kiero.schedule.application.dto;

import jakarta.validation.constraints.NotNull;

public record NowScheduleCompleteRequest(
	@NotNull
	String imageUrl
) {
}
