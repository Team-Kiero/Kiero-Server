package com.kiero;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 기본 클래스
 *
 * 목적: 모든 통합 테스트에서 공통으로 사용할 TestContainers 설정
 *
 * 사용법:
 * - 통합 테스트를 작성할 때 이 클래스를 상속
 * - MySQL 컨테이너가 자동으로 실행됨
 * - 테스트 간에 컨테이너를 재사용하여 속도 향상
 */
@SpringBootTest
@ActiveProfiles("test")  // application-test.yml 사용
@Testcontainers
public abstract class BaseIntegrationTest {
    @Container
    protected static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        // JDBC URL: jdbc:mysql://localhost:랜덤포트/testdb
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);

        // Username: test
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);

        // Password: test
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
    }
}