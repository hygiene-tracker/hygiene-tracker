package com.hygienetracker.api.error;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The error body every endpoint returns (RFC 9457 Problem Details), as described in
 * docs/error-model.md and the {@code Problem} schema in docs/openapi.yaml.
 *
 * @param type      always {@code about:blank}: the status code is the error type
 * @param title     the standard name of the status code
 * @param status    the HTTP status code
 * @param detail    the message to show the user
 * @param instance  the request path that failed
 * @param requestId an ID for finding the request in the logs
 * @param errors    one entry per invalid field, for validation errors only; omitted otherwise
 */
public record Problem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String requestId,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<FieldError> errors) {

    /** One invalid field in a validation error. */
    public record FieldError(String field, String message) {}

    /**
     * Builds an error response with the {@code application/problem+json} content type.
     * The request ID is random until a request-ID filter ties it to the request's log line.
     */
    public static ResponseEntity<Problem> response(HttpStatus status, String detail, String instance) {
        Problem body = new Problem(
                "about:blank",
                status.getReasonPhrase(),
                status.value(),
                detail,
                instance,
                UUID.randomUUID().toString(),
                null);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
