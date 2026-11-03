package dev.will.eventsmanager.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Runs the @Scheduled jobs (the outbox relay). Tests turn it off, so no job runs in the background of a test;
// a test that needs the relay calls it directly.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
