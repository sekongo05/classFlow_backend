package com.mosek.classFlow.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepte 100% des exceptions applicatives et techniques.
 * Ne renvoie jamais de stack trace, message SQL ou nom de classe Java aux clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClassFlowException.class)
    public ResponseEntity<ApiErrorResponse> handleClassFlow(ClassFlowException ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] Business error ({}): {}", traceId, ex.getErrorCode(), ex.getMessage());
        return respond(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), req, traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String traceId = traceId();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valeur invalide.");
        }
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Certains champs envoyés sont invalides.",
                req.getRequestURI(),
                traceId,
                fieldErrors
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String traceId = traceId();
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.putIfAbsent(field, v.getMessage());
        });
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Certains paramètres de la requête sont invalides.",
                req.getRequestURI(),
                traceId,
                fieldErrors
        ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadFormat(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] Invalid request format on {}: {}", traceId, req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_FORMAT", "Le format des données envoyées est invalide.", req, traceId);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String traceId = traceId();
        log.error("[{}] Database integrity violation on {}", traceId, req.getRequestURI(), ex);
        String raw = ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null
                ? ex.getMostSpecificCause().getMessage().toLowerCase()
                : "";
        if (raw.contains("23503") || raw.contains("foreign key") || raw.contains("referential integrity")) {
            return respond(HttpStatus.CONFLICT, "RESOURCE_IN_USE",
                    "Cette ressource est liée à d'autres données et ne peut pas être modifiée ou supprimée.", req, traceId);
        }
        return respond(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE",
                "Cette donnée existe déjà ou viole une contrainte d'unicité.", req, traceId);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class, JwtException.class})
    public ResponseEntity<ApiErrorResponse> handleAuth(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] Auth error on {}: {}", traceId, req.getRequestURI(), ex.getMessage());
        return respond(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Identifiants invalides ou session expirée.", req, traceId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] Access denied on {}", traceId, req.getRequestURI());
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Vous n'avez pas les droits nécessaires pour effectuer cette action.", req, traceId);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        String traceId = traceId();
        return respond(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Le fichier dépasse la taille maximale autorisée.", req, traceId);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        String traceId = traceId();
        return respond(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
                "La ressource demandée est introuvable.", req, traceId);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        String traceId = traceId();
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Méthode HTTP non supportée pour cette ressource.", req, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.error("[{}] Unexpected error on {}", traceId, req.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Une erreur inattendue est survenue. Veuillez réessayer plus tard.", req, traceId);
    }

    private ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String code, String message, HttpServletRequest req, String traceId) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status.value(), code, message, req.getRequestURI(), traceId));
    }

    private String traceId() {
        return UUID.randomUUID().toString();
    }
}
