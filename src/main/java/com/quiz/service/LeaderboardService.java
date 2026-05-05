package com.quiz.service;

import com.quiz.config.CacheConfiguration;
import com.quiz.dto.LeaderboardEntryResponse;
import com.quiz.exception.ApiException;
import com.quiz.model.LeaderboardPeriod;
import com.quiz.repository.QuizRepository;
import com.quiz.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard with cache-aside; Redis key logical shape {@code quizId:period} via Spring cache key.
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private static final int DEFAULT_LIMIT = 100;

    private final ResultRepository resultRepository;
    private final QuizRepository quizRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = CacheConfiguration.LEADERBOARD_CACHE, key = "#quizId + ':' + #period")
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard(Long quizId, LeaderboardPeriod period) {
        if (!quizRepository.existsById(quizId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        Instant submittedFrom = period.submittedFrom(Instant.now());
        List<Object[]> rows = resultRepository.findLeaderboardRows(quizId, submittedFrom, DEFAULT_LIMIT);
        List<LeaderboardEntryResponse> out = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] row : rows) {
            out.add(new LeaderboardEntryResponse(
                    rank++,
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).doubleValue(),
                    ((Number) row[5]).doubleValue(),
                    ((Number) row[6]).doubleValue()
            ));
        }
        return out;
    }

    public void invalidateLeaderboard(Long quizId) {
        Cache cache = cacheManager.getCache(CacheConfiguration.LEADERBOARD_CACHE);
        if (cache == null) {
            return;
        }
        for (LeaderboardPeriod p : LeaderboardPeriod.values()) {
            cache.evict(quizId + ":" + p);
        }
    }
}
