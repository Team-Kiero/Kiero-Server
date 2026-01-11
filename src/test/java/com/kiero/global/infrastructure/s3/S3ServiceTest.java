package com.kiero.global.infrastructure.s3;

import com.kiero.global.infrastructure.s3.dto.PresignedUrlRequest;
import com.kiero.global.infrastructure.s3.dto.PresignedUrlResponse;
import com.kiero.global.infrastructure.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S3Service 단위 테스트
 *
 * 목적: S3Service의 비즈니스 로직을 독립적으로 테스트
 *
 * 특징:
 * - @ExtendWith(MockitoExtension.class): Mockito 사용
 * - @Mock: 의존성을 Mock 객체로 대체
 * - @InjectMocks: Mock 객체들을 주입받는 테스트 대상
 *
 * 테스트 전략:
 * - 실제 AWS S3 연결 없이 테스트 (빠르고 안정적)
 * - Mock 객체로 S3 동작을 시뮬레이션
 * - Presigned URL 생성 로직에 집중
 */
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        // @Value로 주입되는 bucketName을 테스트용으로 설정
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);
    }

    /**
     * 테스트: Presigned 업로드 URL 생성 성공
     *
     * 검증 사항:
     * 1. Presigned URL이 정상적으로 생성되는가?
     * 2. 파일명과 URL이 모두 반환되는가?
     * 3. S3Presigner가 호출되는가?
     */
    @Test
    void generatePresignedUploadUrl_shouldReturnValidUrl() throws Exception {
        // Given
        String originalFileName = "photo.jpg";
        String contentType = "image/jpeg";
        URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

        // Mock PresignedPutObjectRequest
        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(mockUrl);

        // S3Presigner가 호출되면 Mock 객체 반환
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .thenReturn(mockPresignedRequest);

        PresignedUrlRequest request = new PresignedUrlRequest(originalFileName, contentType);

        // When
        PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(request, "schedule");

        // Then
        assertNotNull(response, "응답이 null이면 안됨");
        assertNotNull(response.presignedUrl(), "Presigned URL이 null이면 안됨");
        assertNotNull(response.fileName(), "파일명이 null이면 안됨");
        assertTrue(response.fileName().startsWith("schedule/"),"S3 경로 포함");
        assertTrue(response.fileName().contains(originalFileName), "원본 파일명 포함");

        // S3Presigner.presignPutObject가 1번 호출되었는지 확인
        verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
    }
}
