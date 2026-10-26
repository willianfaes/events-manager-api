package dev.will.eventsmanager;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One Postgres for the whole test run: the class is loaded once, so the container starts once, and the JVM
 * stops it at exit. Tests keep their data apart by rolling back (or, when they commit, by cleaning up after).
 */
public final class SharedPostgres {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    private SharedPostgres() {}

    public static void register(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
