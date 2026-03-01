package com.jfontdev.trackstack;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

/**
 * Base class for full-stack integration tests.
 *
 * <p>This class wires Testcontainers into Spring Boot, boots the application on
 * a random port, and configures Rest Assured to target that port. It also
 * executes a cleanup script after each test to keep data isolated and results
 * reproducible.</p>
 *
 * <p>In addition to the database cleanup performed by the {@code @Sql} annotation,
 * all Redis-backed caches are cleared after every test. This prevents a later test
 * from reading stale cached data that was populated by an earlier test — an issue
 * that would otherwise arise because {@link org.springframework.cache.annotation.Cacheable}
 * methods serve results directly from Redis without touching the database, even after
 * the database has been truncated.</p>
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/db/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class BaseIntegrationTest {

    // Spring Boot will inject the random port assigned to the embedded server
    @LocalServerPort
    private int port;

    @Autowired
    private CacheManager cacheManager;

    /**
     * Configures Rest Assured with the random server port assigned by Spring.
     */
    @BeforeEach
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    /**
     * Clears all Redis-backed caches after each test.
     *
     * <p>The {@code @Sql} cleanup script truncates the database tables, but cached
     * entries in Redis remain alive until their TTL expires. Without this step, a
     * {@link org.springframework.cache.annotation.Cacheable} read executed in a
     * subsequent test would return the stale cached value instead of querying the
     * (now empty) database, breaking test isolation. Iterating over every cache
     * name registered in the {@link CacheManager} ensures that all caches —
     * {@code tracks}, {@code playlists}, {@code tags}, and any future caches —
     * are evicted consistently.</p>
     */
    @AfterEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}

