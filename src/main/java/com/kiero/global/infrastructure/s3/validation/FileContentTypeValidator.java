package com.kiero.global.infrastructure.s3.validation;

import com.kiero.global.infrastructure.s3.enums.AllowedFileType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

// 파일 Content-Type 검증 Validator
public class FileContentTypeValidator implements ConstraintValidator<ValidFileContentType, String> {

    private Set<String> allowedContentTypes;

    @Override
    public void initialize(ValidFileContentType annotation) {
        allowedContentTypes = AllowedFileType.mergeContentTypes(annotation.value());
    }

    @Override
    public boolean isValid(String contentType, ConstraintValidatorContext context) {
        // null이나 빈 값은 @NotBlank에서 처리
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String normalizedContentType = contentType.toLowerCase().trim();

        return allowedContentTypes.contains(normalizedContentType);
    }
}
