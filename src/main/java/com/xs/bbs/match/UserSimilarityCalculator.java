package com.xs.bbs.match;

import com.xs.bbs.config.MatchProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserSimilarityCalculator {

    private final MatchProperties matchProperties;

    public UserSimilarityCalculator(MatchProperties matchProperties) {
        this.matchProperties = matchProperties;
    }

    public UserSimilaritySnapshot calculate(Long sourceUserId, Long targetUserId, List<ThoughtSimilarity> similarities) {
        if (similarities.isEmpty()) {
            return new UserSimilaritySnapshot(sourceUserId, targetUserId, 0, 0, UserVisibilityLevel.NONE);
        }

        double averageThoughtScore = similarities.stream()
                .mapToDouble(ThoughtSimilarity::finalScore)
                .average()
                .orElse(0D);
        double averageDegreeScore = similarities.stream()
                .mapToDouble(ThoughtSimilarity::degreeScore)
                .average()
                .orElse(0D);
        int score = (int) Math.min(
                100,
                similarities.size() * 12
                        + averageThoughtScore * 40
                        + averageDegreeScore * 8
        );
        UserVisibilityLevel visibilityLevel = resolveVisibility(score);
        return new UserSimilaritySnapshot(sourceUserId, targetUserId, score, similarities.size(), visibilityLevel);
    }

    private UserVisibilityLevel resolveVisibility(int score) {
        if (score >= matchProperties.getRevealThreshold()) {
            return UserVisibilityLevel.REAL_NAME;
        }
        if (score >= 30) {
            return UserVisibilityLevel.ANONYMOUS;
        }
        return UserVisibilityLevel.NONE;
    }
}
