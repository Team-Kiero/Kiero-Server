package com.kiero.global.infrastructure.s3.controller;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.infrastructure.s3.dto.PresignedUrlRequest;
import com.kiero.global.infrastructure.s3.dto.PresignedUrlResponse;
import com.kiero.global.infrastructure.s3.exception.S3SuccessCode;
import com.kiero.global.infrastructure.s3.service.S3Service;
import com.kiero.global.response.dto.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/presigned-url")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/schedules")
    public ResponseEntity<SuccessResponse<PresignedUrlResponse>> generateSchedulePresignedUrl(
            @CurrentMember CurrentAuth currentAuth,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(request, "schedule");
        return ResponseEntity.ok()
                .body(SuccessResponse.of(S3SuccessCode.PRESIGNED_URL_CREATED, response));
    }
}
