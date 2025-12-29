package com.kiero.global.infrastructure.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

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
@DisplayName("S3Service 단위 테스트")
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

    @Nested
    @DisplayName("Presigned 업로드 URL 생성 테스트")
    class GeneratePresignedUploadUrlTests {

        /**
         * 테스트: Presigned 업로드 URL 생성 성공
         *
         * 검증 사항:
         * 1. Presigned URL이 정상적으로 생성되는가?
         * 2. 파일명과 URL이 모두 반환되는가?
         * 3. S3Presigner가 호출되는가?
         */
        @Test
        @DisplayName("정상적인 파일명과 Content-Type으로 Presigned URL 생성 성공")
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

            // When
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(
                    originalFileName, contentType
            );

            // Then
            assertNotNull(response, "응답이 null이면 안됨");
            assertNotNull(response.presignedUrl(), "Presigned URL이 null이면 안됨");
            assertNotNull(response.fileName(), "파일명이 null이면 안됨");
            assertTrue(response.fileName().contains(originalFileName), "원본 파일명 포함");

            // S3Presigner.presignPutObject가 1번 호출되었는지 확인
            verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
        }

        @Test
        @DisplayName("생성된 파일명은 UUID를 포함하여 고유해야 함")
        void generatePresignedUploadUrl_shouldGenerateUniqueFileName() throws Exception {
            // Given
            String originalFileName = "test.jpg";
            String contentType = "image/jpeg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            S3Service.PresignedUrlResponse response1 = s3Service.generatePresignedUploadUrl(
                    originalFileName, contentType
            );
            S3Service.PresignedUrlResponse response2 = s3Service.generatePresignedUploadUrl(
                    originalFileName, contentType
            );

            // Then
            assertNotEquals(response1.fileName(), response2.fileName(),
                    "같은 원본 파일명이라도 생성된 파일명은 달라야 함 (UUID 사용)");

            assertTrue(response1.fileName().contains("_"), "파일명에 구분자(_)가 있어야 함");
            assertTrue(response2.fileName().contains("_"), "파일명에 구분자(_)가 있어야 함");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "image/jpeg",
                "image/png",
                "image/gif",
                "video/mp4",
                "application/pdf"
        })
        @DisplayName("다양한 Content-Type으로 URL 생성 가능")
        void generatePresignedUploadUrl_shouldHandleVariousContentTypes(String contentType) throws Exception {
            // Given
            String originalFileName = "test-file.dat";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(
                    originalFileName, contentType
            );

            // Then
            assertNotNull(response);
            assertNotNull(response.presignedUrl());

            // Content-Type이 올바르게 전달되었는지 확인
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());

            assertEquals(contentType, captor.getValue().putObjectRequest().contentType(),
                    "Content-Type이 올바르게 설정되어야 함");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "test-file-with-dashes.jpg",
                "test_file_with_underscores.png",
                "한글파일명.jpg",
                "file with spaces.mp4",
                "file.multiple.dots.pdf"
        })
        @DisplayName("다양한 파일명 형식 처리")
        void generatePresignedUploadUrl_shouldHandleVariousFileNameFormats(String fileName) throws Exception {
            // Given
            String contentType = "application/octet-stream";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(
                    fileName, contentType
            );

            // Then
            assertNotNull(response);
            assertNotNull(response.fileName());
            assertTrue(response.fileName().contains(fileName),
                    "원본 파일명이 생성된 파일명에 포함되어야 함");
        }

        @Test
        @DisplayName("매우 긴 파일명도 처리 가능")
        void generatePresignedUploadUrl_shouldHandleLongFileName() throws Exception {
            // Given
            String longFileName = "a".repeat(200) + ".jpg";
            String contentType = "image/jpeg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(
                    longFileName, contentType
            );

            // Then
            assertNotNull(response);
            assertNotNull(response.fileName());
        }

        @Test
        @DisplayName("S3Presigner 호출 시 올바른 버킷명 사용")
        void generatePresignedUploadUrl_shouldUseCorrectBucketName() throws Exception {
            // Given
            String originalFileName = "test.jpg";
            String contentType = "image/jpeg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            s3Service.generatePresignedUploadUrl(originalFileName, contentType);

            // Then
            ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
            verify(s3Presigner).presignPutObject(captor.capture());

            assertEquals(bucketName, captor.getValue().putObjectRequest().bucket(),
                    "설정된 버킷명이 사용되어야 함");
        }
    }

    @Nested
    @DisplayName("Presigned 조회 URL 생성 테스트")
    class GeneratePresignedViewUrlTests {

        @Test
        @DisplayName("정상적인 파일명으로 조회 URL 생성 성공")
        void generatePresignedViewUrl_shouldReturnValidUrl() throws Exception {
            // Given
            String fileName = "uuid-123_photo.jpg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/view-url");

            PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            String viewUrl = s3Service.generatePresignedViewUrl(fileName);

            // Then
            assertNotNull(viewUrl, "조회 URL이 null이면 안됨");
            assertEquals(mockUrl.toString(), viewUrl, "생성된 URL이 예상과 일치해야 함");

            verify(s3Presigner, times(1)).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("조회 URL 생성 시 올바른 버킷과 파일명 사용")
        void generatePresignedViewUrl_shouldUseCorrectBucketAndKey() throws Exception {
            // Given
            String fileName = "test-file.jpg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/view-url");

            PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            s3Service.generatePresignedViewUrl(fileName);

            // Then
            ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
            verify(s3Presigner).presignGetObject(captor.capture());

            assertEquals(bucketName, captor.getValue().getObjectRequest().bucket(),
                    "설정된 버킷명이 사용되어야 함");
            assertEquals(fileName, captor.getValue().getObjectRequest().key(),
                    "전달된 파일명이 그대로 사용되어야 함");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "uuid_photo.jpg",
                "path/to/file.png",
                "deep/nested/path/video.mp4"
        })
        @DisplayName("다양한 파일 경로 형식으로 조회 URL 생성")
        void generatePresignedViewUrl_shouldHandleVariousFilePaths(String fileName) throws Exception {
            // Given
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/view-url");

            PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            String viewUrl = s3Service.generatePresignedViewUrl(fileName);

            // Then
            assertNotNull(viewUrl);
            assertEquals(mockUrl.toString(), viewUrl);
        }

        @Test
        @DisplayName("여러 번 호출해도 일관된 결과 반환")
        void generatePresignedViewUrl_shouldBeIdempotent() throws Exception {
            // Given
            String fileName = "test.jpg";
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/view-url");

            PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            String url1 = s3Service.generatePresignedViewUrl(fileName);
            String url2 = s3Service.generatePresignedViewUrl(fileName);

            // Then
            assertEquals(url1, url2, "같은 파일에 대한 조회 URL은 동일해야 함 (Mock 환경)");
        }
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("S3Presigner가 예외를 던지면 전파되어야 함 - 업로드")
        void generatePresignedUploadUrl_shouldPropagateException() {
            // Given
            String fileName = "test.jpg";
            String contentType = "image/jpeg";

            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("S3 연결 실패"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                s3Service.generatePresignedUploadUrl(fileName, contentType);
            }, "S3Presigner 예외가 전파되어야 함");
        }

        @Test
        @DisplayName("S3Presigner가 예외를 던지면 전파되어야 함 - 조회")
        void generatePresignedViewUrl_shouldPropagateException() {
            // Given
            String fileName = "test.jpg";

            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenThrow(new RuntimeException("S3 연결 실패"));

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                s3Service.generatePresignedViewUrl(fileName);
            }, "S3Presigner 예외가 전파되어야 함");
        }

        @Test
        @DisplayName("잘못된 URL이 반환되어도 예외 없이 처리")
        void generatePresignedUploadUrl_shouldHandleInvalidUrlGracefully() throws Exception {
            // Given
            String fileName = "test.jpg";
            String contentType = "image/jpeg";

            // 정상적인 URL 반환 (실제로 잘못된 URL이어도 String으로는 처리 가능)
            URL mockUrl = new URL("https://invalid-url.example.com");

            PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(mockPresignedRequest);

            // When
            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(
                    fileName, contentType
            );

            // Then
            assertNotNull(response);
            assertNotNull(response.presignedUrl());
        }
    }

    @Nested
    @DisplayName("PresignedUrlResponse 레코드 테스트")
    class PresignedUrlResponseTests {

        @Test
        @DisplayName("PresignedUrlResponse 생성 및 접근")
        void presignedUrlResponse_shouldCreateAndAccessCorrectly() {
            // Given
            String url = "https://test-bucket.s3.amazonaws.com/presigned-url";
            String fileName = "uuid_photo.jpg";

            // When
            S3Service.PresignedUrlResponse response = new S3Service.PresignedUrlResponse(url, fileName);

            // Then
            assertEquals(url, response.presignedUrl());
            assertEquals(fileName, response.fileName());
        }

        @Test
        @DisplayName("PresignedUrlResponse equals 및 hashCode")
        void presignedUrlResponse_shouldImplementEqualsAndHashCode() {
            // Given
            String url = "https://test.com/url";
            String fileName = "test.jpg";

            S3Service.PresignedUrlResponse response1 = new S3Service.PresignedUrlResponse(url, fileName);
            S3Service.PresignedUrlResponse response2 = new S3Service.PresignedUrlResponse(url, fileName);
            S3Service.PresignedUrlResponse response3 = new S3Service.PresignedUrlResponse(url, "different.jpg");

            // Then
            assertEquals(response1, response2, "같은 값으로 생성된 레코드는 같아야 함");
            assertNotEquals(response1, response3, "다른 값으로 생성된 레코드는 달라야 함");
            assertEquals(response1.hashCode(), response2.hashCode(), "같은 레코드는 같은 hashCode를 가져야 함");
        }

        @Test
        @DisplayName("PresignedUrlResponse toString")
        void presignedUrlResponse_shouldHaveProperToString() {
            // Given
            String url = "https://test.com/url";
            String fileName = "test.jpg";
            S3Service.PresignedUrlResponse response = new S3Service.PresignedUrlResponse(url, fileName);

            // When
            String toString = response.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains(url), "toString에 URL이 포함되어야 함");
            assertTrue(toString.contains(fileName), "toString에 파일명이 포함되어야 함");
        }
    }
}
