package dev.will.eventsmanager.events.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record EventFilter(
        @Schema(description = "Busca parcial pelo nome", example = "rock")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String name,

        @Schema(description = "Eventos a partir desta data", example = "2026-11-01")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

        @Schema(description = "Eventos até esta data", example = "2026-12-31")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {}
