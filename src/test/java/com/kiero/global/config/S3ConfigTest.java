package com.kiero.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3Config 통합 테스트
 *
 * 목적: S3Config가 Spring 컨텍스트에서 올바르게 Bean을 생성하는지 검증
 *
 * 특징:
 * - @SpringBootTest: 전체 Spring 컨텍스트 로드
 * - @ActiveProfiles("test"): 테스트용 설정 사용
 * - 실제 AWS 연결은 하지 않고 Bean 생성만 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("S3Config 통합 테스트")
class S3ConfigTest {

    @Autowired(required = false)
    private S3Presigner s3Presigner;

    @Test
    @DisplayName("S3Presigner Bean이 정상적으로 생성되어야 함")
    void s3PresignerBean_shouldBeCreated() {
        // Then
        assertNotNull(s3Presigner, "S3Presigner Bean이 생성되어야 함");
    }

    @Test
    @DisplayName("생성된 S3Presigner는 사용 가능한 상태여야 함")
    void s3PresignerBean_shouldBeUsable() {
        // Then
        assertNotNull(s3Presigner, "S3Presigner가 null이면 안됨");
        assertDoesNotThrow(() -> {
            // Bean이 정상적으로 생성되었는지 확인
            // 실제 S3 호출은 하지 않음
            s3Presigner.toString();
        }, "S3Presigner Bean 접근 시 예외가 발생하면 안됨");
    }
}