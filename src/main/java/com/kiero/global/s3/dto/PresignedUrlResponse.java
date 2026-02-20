package com.kiero.global.s3.dto;

public record PresignedUrlResponse(
	String presignedUrl,
	String fileName
) {
}
