package com.trinity.banking.infrastructure.adapter.in.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Formato único de error para toda la API. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
