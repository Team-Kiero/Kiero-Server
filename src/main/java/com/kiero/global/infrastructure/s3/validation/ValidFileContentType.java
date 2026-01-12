package com.kiero.global.infrastructure.s3.validation;

import com.kiero.global.infrastructure.s3.enums.AllowedFileType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

// 파일 Content-Type 검증 어노테이션
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileContentTypeValidator.class)
@Documented
public @interface ValidFileContentType {

    AllowedFileType[] value();

    String message() default "허용되지 않은 파일 타입입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
