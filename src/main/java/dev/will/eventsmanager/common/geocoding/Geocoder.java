package dev.will.eventsmanager.common.geocoding;

import dev.will.eventsmanager.common.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class Geocoder {

    private final NominatimClient nominatimClient;

    // Only coordinates are stored (#result is the value inside the Optional): an address that was not found can be
    // fixed on the map later, and a failure must never look like an answer.
    @Cacheable(cacheNames = CacheConfig.GEOCODING, key = "#address.trim().toLowerCase()", unless = "#result == null")
    public Optional<Coordinates> locate(String address) {
        return nominatimClient.locate(address);
    }
}
