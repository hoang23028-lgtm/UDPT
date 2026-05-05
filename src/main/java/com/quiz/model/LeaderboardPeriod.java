package com.quiz.model;

import com.quiz.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Leaderboard time bucket: calendar week (UTC, Monday start), calendar month (UTC), or all time.
 */
public enum LeaderboardPeriod {
    WEEK,
    MONTH,
    ALL;

    public static LeaderboardPeriod fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        try {
            return LeaderboardPeriod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid period; use WEEK, MONTH, or ALL");
        }
    }

    /**
     * Inclusive lower bound for {@code submissions.submitted_at}, or {@link Instant#EPOCH} for {@link #ALL}.
     */
    public Instant submittedFrom(Instant now) {
        if (this == ALL) {
            return Instant.EPOCH;
        }
        ZonedDateTime z = now.atZone(ZoneOffset.UTC);
        if (this == MONTH) {
            return z.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        return z.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }
}
