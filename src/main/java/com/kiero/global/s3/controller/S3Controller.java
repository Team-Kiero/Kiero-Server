package com.kiero.global.s3.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kiero.global.auth.annotation.CurrentMember;
import com.kiero.global.auth.dto.CurrentAuth;
import com.kiero.global.response.dto.SuccessResponse;
import com.kiero.global.s3.dto.PresignedUrlRequest;
import com.kiero.global.s3.dto.PresignedUrlResponse;
import com.kiero.global.s3.exception.S3SuccessCode;
import com.kiero.global.s3.service.S3Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
