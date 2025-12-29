package com.kiero;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KieroApplication 통합 테스트
 *
 * 목적: Spring Boot 애플리케이션이 정상적으로 시작되는지 검증
 *
 * 특징:
 * - @SpringBootTest: 전체 애플리케이션 컨텍스트 로드
 * - @ActiveProfiles("test"): 테스트 환경 설정 사용
 * - Context Load Test: 모든 Bean이 올바르게 생성되는지 확인
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("KieroApplication 통합 테스트")
class KieroApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Spring 애플리케이션 컨텍스트가 정상적으로 로드되어야 함")
    void contextLoads() {
        // Then
        assertNotNull(applicationContext, "ApplicationContext가 null이면 안됨");
    }

    @Test
    @DisplayName("필수 Bean들이 모두 등록되어야 함")
    void requiredBeans_shouldBeRegistered() {
        // Then
        assertTrue(applicationContext.containsBean("s3Presigner"),
                "S3Presigner Bean이 등록되어야 함");

        // S3Service Bean 확인
        String[] beanNames = applicationContext.getBeanNamesForType(
                com.kiero.global.infrastructure.s3.S3Service.class
        );
        assertTrue(beanNames.length > 0, "S3Service Bean이 최소 1개 이상 등록되어야 함");
    }

    @Test
    @DisplayName("애플리케이션 이름이 설정되어 있어야 함")
    void applicationName_shouldBeConfigured() {
        // Given
        String applicationName = applicationContext.getEnvironment()
                .getProperty("spring.application.name");

        // Then
        assertNotNull(applicationName, "애플리케이션 이름이 설정되어야 함");
        assertEquals("kiero-test", applicationName,
                "테스트 환경의 애플리케이션 이름은 'kiero-test'여야 함");
    }

    @Test
    @DisplayName("활성 프로파일이 'test'여야 함")
    void activeProfile_shouldBeTest() {
        // Given
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();

        // Then
        assertEquals(1, activeProfiles.length, "활성 프로파일은 1개여야 함");
        assertEquals("test", activeProfiles[0], "활성 프로파일은 'test'여야 함");
    }

    @Test
    @DisplayName("AWS 설정값이 테스트 환경에 맞게 설정되어야 함")
    void awsConfiguration_shouldBeLoadedForTest() {
        // Given
        String bucket = applicationContext.getEnvironment().getProperty("aws.s3.bucket");
        String region = applicationContext.getEnvironment().getProperty("aws.region");

        // Then
        assertNotNull(bucket, "S3 버킷명이 설정되어야 함");
        assertNotNull(region, "AWS 리전이 설정되어야 함");
        assertEquals("test-bucket", bucket, "테스트 환경의 버킷명은 'test-bucket'이어야 함");
        assertEquals("ap-northeast-2", region, "리전은 'ap-northeast-2'여야 함");
    }

    @Test
    @DisplayName("JPA 설정이 테스트 환경에 맞게 설정되어야 함")
    void jpaConfiguration_shouldBeLoadedForTest() {
        // Given
        String ddlAuto = applicationContext.getEnvironment()
                .getProperty("spring.jpa.hibernate.ddl-auto");

        // Then
        assertNotNull(ddlAuto, "JPA DDL 설정이 있어야 함");
        assertEquals("create-drop", ddlAuto,
                "테스트 환경에서는 'create-drop'이어야 함");
    }
}