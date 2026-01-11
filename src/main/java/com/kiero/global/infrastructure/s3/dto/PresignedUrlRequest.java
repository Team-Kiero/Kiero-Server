package com.kiero.global.infrastructure.s3.dto;

import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
	@NotBlank(message = "파일명을 입력해주세요.")
	String fileName,

	@NotBlank(message = "Content Type을 입력해주세요.")
	String contentType
) {
}
