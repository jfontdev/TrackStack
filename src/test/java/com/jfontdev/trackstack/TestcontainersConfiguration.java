package com.jfontdev.trackstack;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for integration tests.
 *
 * <p>Provides real infrastructure containers (PostgreSQL and Redis) that Spring Boot
 * automatically wires into the application context via {@link ServiceConnection}.
 * This means tests run against actual services rather than mocks, giving higher
 * confidence that caching, persistence, and queries behave correctly end-to-end.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    /**
     * Starts a PostgreSQL container and registers it as the test datasource.
     *
     * <p>The container uses a random host port assigned by Testcontainers to avoid
     * port conflicts when multiple test contexts start in the same JVM run.
     * {@link ServiceConnection} automatically wires the dynamic JDBC URL into the
     * application context.</p>
     *
     * @return a configured {@link PostgreSQLContainer} ready for test use
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("trackstack_test")
                .withUsername("trackstack")
                .withPassword("trackstack");
    }

    /**
     * Starts a Redis container and registers it as the test Redis connection.
     *
     * <p>Because caching is backed by Redis, integration tests need a real Redis
     * instance to verify that {@link org.springframework.cache.annotation.Cacheable}
     * and {@link org.springframework.cache.annotation.CacheEvict} work correctly.
     * {@link ServiceConnection} detects the {@code redis} image name and automatically
     * configures {@code spring.data.redis.host} and {@code spring.data.redis.port}.</p>
     *
     * @return a configured {@link GenericContainer} running Redis, ready for test use
     */
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis/redis-stack:latest"))
                .withExposedPorts(6379);
    }

}
