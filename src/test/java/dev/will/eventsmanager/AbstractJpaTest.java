package dev.will.eventsmanager;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// One configuration for every repository test, so Spring builds a single context (and connection pool) for all of them.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractJpaTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
    }
}
