package com.mosek.classFlow.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Format uniforme de réponse d'erreur API — n'expose jamais de détails techniques.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String errorCode, String message, String path, String traceId) {
        return new ApiErrorResponse(Instant.now(), status, errorCode, message, path, traceId, Map.of());
    }

    public static ApiErrorResponse of(int status, String errorCode, String message, String path, String traceId, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, errorCode, message, path, traceId, fieldErrors == null ? Map.of() : fieldErrors);
    }
}
