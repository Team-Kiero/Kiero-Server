package com.kiero.schedule.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record CompleteNowScheduleRequest(
	@NotNull
	String imageUrl
) {
}
