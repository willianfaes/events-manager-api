package dev.will.eventsmanager;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static java.util.Objects.requireNonNull;

@AutoConfigureMockMvc
@SpringBootTest
public abstract class AbstractIntegrationTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine");

    static {
        redis.addExposedPort(6379);
        postgres.start();
        redis.start();
    }

    @Autowired
    private CacheManager cacheManager;

    // Unlike the database, Redis is not rolled back between tests.
    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> requireNonNull(cacheManager.getCache(name)).clear());
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> "integration-test-secret-with-32-bytes-or-more");
    }
}
