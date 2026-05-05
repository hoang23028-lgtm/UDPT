package com.quiz.util;

import com.quiz.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Resolves the submission idempotency token from {@code Idempotency-Key} (preferred) or JSON body.
 */
public final class SubmitIdempotencyKeys {

    public static final int MAX_LENGTH = 128;

    private SubmitIdempotencyKeys() {
    }

    /**
     * Header wins when present; header and body must agree when both are set.
     *
     * @return normalized key or {@code null} when neither source provides a value
     */
    public static String resolve(String idempotencyKeyHeader, String idempotencyKeyBody) {
        String header = idempotencyKeyHeader == null ? null : idempotencyKeyHeader.trim();
        boolean headerPresent = header != null && !header.isEmpty();
        if (headerPresent && header.length() > MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Idempotency-Key exceeds " + MAX_LENGTH + " characters");
        }
        if (headerPresent && idempotencyKeyBody != null && !header.equals(idempotencyKeyBody)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header and body idempotencyKey must match when both are sent");
        }
        if (headerPresent) {
            return header;
        }
        return idempotencyKeyBody;
    }
}
