package com.kiero.global.infrastructure.s3.dto;

import com.kiero.global.infrastructure.s3.enums.AllowedFileType;
import com.kiero.global.infrastructure.s3.validation.ValidFileContentType;
import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
	@NotBlank(message = "파일명을 입력해주세요.")
	String fileName,

	@NotBlank(message = "Content Type을 입력해주세요.")
    @ValidFileContentType(value = AllowedFileType.IMAGE, message = "이미지 파일만 업로드 가능합니다.")
	String contentType
) {
}
