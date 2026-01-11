package com.kiero.global.infrastructure.s3.dto;

public record PresignedUrlResponse(
	String presignedUrl,
	String fileName
) {
}
