package dev.will.eventsmanager.common.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.will.eventsmanager.common.PageResponse;
import dev.will.eventsmanager.common.geocoding.Coordinates;
import dev.will.eventsmanager.events.dto.response.EventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String EVENTS_LISTING = "events-listing";
    public static final String GEOCODING = "geocoding";

    // The listing carries remainingSpots, which is only a snapshot: the TTL is the staleness bound.
    static final Duration EVENTS_LISTING_TTL = Duration.ofSeconds(30);

    // An address rarely moves, and Nominatim limits us to one request per second.
    static final Duration GEOCODING_TTL = Duration.ofDays(30);

    // The cache is an optimization: when Redis fails, skip it and let the call go to the source.
    // One WARN line, no stack trace (same convention as GlobalExceptionHandler); the key is left out because it holds the filter.
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                warn("get", cache, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                warn("put", cache, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                warn("evict", cache, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                warn("clear", cache, exception);
            }

            private void warn(String operation, Cache cache, RuntimeException exception) {
                log.warn("Cache '{}' {} failed, skipping it: {}",
                        cache.getName(), operation, exception.getClass().getSimpleName());
            }
        };
    }

    @Bean
    RedisCacheManagerBuilderCustomizer redisCaches(ObjectMapper objectMapper) {
        // A typed serializer (no default typing in the JSON) keeps the stored value free of class names
        // and reads it back as PageResponse<EventResponse>, records included.
        JavaType listingType = objectMapper.getTypeFactory()
                .constructParametricType(PageResponse.class, EventResponse.class);
        RedisCacheConfiguration eventsListing = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(EVENTS_LISTING_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, listingType)));
        RedisCacheConfiguration geocoding = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(GEOCODING_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, Coordinates.class)));

        // transactionAware: writes and evictions happen after the commit, so a rolled-back change never
        // reaches the cache and an eviction is not undone by a read that still sees the old row.
        return builder -> builder
                .transactionAware()
                .withCacheConfiguration(EVENTS_LISTING, eventsListing)
                .withCacheConfiguration(GEOCODING, geocoding);
    }
}
