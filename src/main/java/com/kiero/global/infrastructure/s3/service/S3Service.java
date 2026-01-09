package com.kiero.global.infrastructure.s3.service;

import com.kiero.global.infrastructure.s3.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Presigned URL 생성 메서드 (업로드용)
     *
     * 동작 원리:
     * 1. 고유한 파일명 생성 (UUID + 원본 파일명)
     * 2. S3에 PUT 요청을 할 수 있는 서명된 URL 생성
     * 3. 이 URL은 우리의 AWS 인증 정보로 암호화되어 있음
     * 4. 클라이언트는 이 URL로 직접 S3에 업로드 가능
     *
     * @param originalFileName 원본 파일명
     * @param contentType 파일의 MIME 타입 (예: image/jpeg, video/mp4)
     * @return Presigned URL과 생성된 파일명을 담은 객체
     */
    public PresignedUrlResponse generatePresignedUploadUrl(String originalFileName, String contentType) {
        // 1. 고유한 파일명 생성 (중복 방지)
        String fileName = generateFileName(originalFileName);

        // 2. S3에 업로드할 객체의 정보를 설정
        //    - bucket: 어느 버킷에 업로드할지
        //    - key: S3 내에서의 파일 경로/이름
        //    - contentType: 파일 타입
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        // 3. Presigned URL 요청 생성
        //    - signatureDuration: URL 유효 시간 (1시간)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofHours(1))
                .build();

        // 4. 실제 서명된 URL 생성
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        // 5. 생성된 URL 추출
        //    이 URL에는 다음 정보가 포함됨:
        //    - 버킷명, 파일명
        //    - 만료 시간
        //    - 암호화된 서명
        String presignedUrl = presignedRequest.url().toString();

        // 6. 클라이언트에게 반환할 정보
        return new PresignedUrlResponse(presignedUrl, fileName);
    }

    /**
     * Presigned URL 생성 메서드 (조회용)
     *
     * 업로드와 유사하지만 GET 요청용 URL을 생성
     *
     * 왜 10분으로 짧게 설정하나?
     * - 조회는 즉시 실행되므로 긴 시간 불필요
     * - 보안: URL 유출 시 피해 최소화
     * - 업로드(1시간)보다 짧은 이유: 업로드는 대용량 파일 고려
     *
     * 사용 예시:
     * - 피드에서 아이 도착 사진 표시
     * - <img src={presignedUrl} /> 형태로 사용
     * - 다운로드가 아닌 화면 표시용
     *
     * @param fileName S3에 저장된 파일명
     * @return 조회용 Presigned URL (10분 유효)
     */
    public String generatePresignedViewUrl(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        // 다운로드 URL은 짧게 설정 (10분)
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(10))
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    // UUID를 사용해 고유한 파일명 생성
    private String generateFileName(String originalFileName) {
        return UUID.randomUUID() + "_" + originalFileName;
    }
}
