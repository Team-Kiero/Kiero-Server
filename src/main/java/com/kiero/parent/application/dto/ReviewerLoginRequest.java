package com.kiero.parent.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Reviewer login request")
public record ReviewerLoginRequest(
	@Schema(description = "Reviewer parent login password", example = "your-reviewer-login-password")
	@NotBlank(message = "Reviewer password is required.")
	String password
) {
}
