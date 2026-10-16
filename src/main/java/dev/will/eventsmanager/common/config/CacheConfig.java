package dev.will.eventsmanager.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EVENTS_LISTING = "events-listing";

    // The listing carries remainingSpots, which is only a snapshot: the TTL is the staleness bound.
    static final Duration EVENTS_LISTING_TTL = Duration.ofSeconds(30);

    @Bean
    RedisCacheManagerBuilderCustomizer eventsListingCache(ObjectMapper objectMapper) {
        // A typed serializer (no default typing in the JSON) keeps the stored value free of class names
        // and reads it back as PageResponse<EventResponse>, records included.
        JavaType listingType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, EventResponse.class);
        RedisCacheConfiguration eventsListing = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(EVENTS_LISTING_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, listingType)));

        // transactionAware: writes and evictions happen after the commit, so a rolled-back change never
        // reaches the cache and an eviction is not undone by a read that still sees the old row.
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(EVENTS_LISTING, eventsListing);
    }
}
