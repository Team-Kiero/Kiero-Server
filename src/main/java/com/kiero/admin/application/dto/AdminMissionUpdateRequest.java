package com.kiero.admin.application.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminMissionUpdateRequest(
	@NotBlank(message = "미션 이름은 필수입니다.")
	String name,

	@Positive(message = "보상 코인은 양수여야 합니다.")
	int reward,

	@NotNull(message = "마감일은 필수입니다.")
	LocalDate dueAt
) {
}
