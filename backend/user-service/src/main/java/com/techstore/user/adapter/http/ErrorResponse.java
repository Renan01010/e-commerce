package com.techstore.user.adapter.http;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String message,
        String details,
        String correlationId,
        Instant timestamp) {}