package com.kiero.global.infrastructure.s3.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.kiero.global.infrastructure.s3.enums.AllowedFileType;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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
