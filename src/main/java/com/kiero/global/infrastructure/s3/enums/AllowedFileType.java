package com.kiero.global.infrastructure.s3.enums;

import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum AllowedFileType {
    IMAGE(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    ),
    VIDEO(
        "video/mp4",
        "video/webm",
        "video/ogg",
        "video/quicktime"
    ),
    DOCUMENT(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Set<String> contentTypes;

    AllowedFileType(String... contentTypes) {
        this.contentTypes = Set.of(contentTypes);
    }

    public static Set<String> mergeContentTypes(AllowedFileType... types) {
        return Stream.of(types)
            .flatMap(type -> type.getContentTypes().stream())
            .collect(Collectors.toUnmodifiableSet());
    }
}
