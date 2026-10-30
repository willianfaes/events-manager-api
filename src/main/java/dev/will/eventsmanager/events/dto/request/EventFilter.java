package dev.will.eventsmanager.events.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EventFilter(
        @Schema(description = "Part of the event name")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String name,

        @Schema(description = "Events from this date on, format yyyy-MM-dd (default: today)")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

        @Schema(description = "Events up to this date, format yyyy-MM-dd")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
    public EventFilter {
        from = from != null ? from : LocalDate.now();
    }
}
