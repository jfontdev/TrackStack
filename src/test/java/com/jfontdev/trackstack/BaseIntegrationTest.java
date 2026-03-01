package com.jfontdev.trackstack;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

/**
 * Base class for full-stack integration tests.
 *
 * <p>This class wires Testcontainers into Spring Boot, boots the application on
 * a random port, and configures Rest Assured to target that port. It also
 * executes a cleanup script after each test to keep data isolated and results
 * reproducible.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseIntegrationTest {

    // Spring Boot will inject the random port assigned to the embedded server
    @LocalServerPort
    private int port;

    /**
     * Configures Rest Assured with the random server port assigned by Spring.
     */
    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }
}

