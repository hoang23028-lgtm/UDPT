package com.quiz.service;

import com.quiz.entity.Quiz;
import com.quiz.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

public final class TimeBonusCalculator {

    private TimeBonusCalculator() {
    }

    /**
     * Linear bonus: full {@code timeBonusMax} at start, zero at {@code timeLimitSeconds} elapsed.
     *
     * @param failOnExceeded if true, elapsed beyond limit yields HTTP bad request; if false, bonus is 0 (reconcile).
     */
    public static double computeTimeBonus(
            Quiz quiz,
            Instant attemptStartedAt,
            Instant submittedAt,
            boolean failOnExceeded
    ) {
        Long limitSec = quiz.getTimeLimitSeconds();
        Double maxBonus = quiz.getTimeBonusMax();
        if (limitSec == null || limitSec <= 0 || maxBonus == null || maxBonus <= 0) {
            return 0.0;
        }
        if (attemptStartedAt == null) {
            return 0.0;
        }
        long elapsedSec = Duration.between(attemptStartedAt, submittedAt).getSeconds();
        if (elapsedSec < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "attemptStartedAt must be before submission time");
        }
        if (elapsedSec > limitSec) {
            if (failOnExceeded) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Attempt exceeded time limit of " + limitSec + " seconds");
            }
            return 0.0;
        }
        double ratio = (limitSec - elapsedSec) / (double) limitSec;
        return Math.round(maxBonus * ratio * 100.0) / 100.0;
    }

    public static double rankScore(long correctScore, double timeBonus) {
        return correctScore + timeBonus;
    }
}
