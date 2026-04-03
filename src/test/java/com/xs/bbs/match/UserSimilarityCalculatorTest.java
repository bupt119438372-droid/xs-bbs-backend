package com.xs.bbs.match;

import com.xs.bbs.config.MatchProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserSimilarityCalculatorTest {

    @Test
    void shouldStayAnonymousWhenScoreBelowRevealThreshold() {
        MatchProperties properties = new MatchProperties();
        properties.setRevealThreshold(60);
        UserSimilarityCalculator calculator = new UserSimilarityCalculator(properties);

        UserSimilaritySnapshot snapshot = calculator.calculate(
                1L,
                2L,
                List.of(
                        new ThoughtSimilarity(0.31D, 1D, 0.517D),
                        new ThoughtSimilarity(0.38D, 1D, 0.566D)
                )
        );

        assertThat(snapshot.score()).isBetween(30, 59);
        assertThat(snapshot.visibilityLevel()).isEqualTo(UserVisibilityLevel.ANONYMOUS);
    }

    @Test
    void shouldRevealRealNameWhenScoreHitsThreshold() {
        MatchProperties properties = new MatchProperties();
        properties.setRevealThreshold(60);
        UserSimilarityCalculator calculator = new UserSimilarityCalculator(properties);

        UserSimilaritySnapshot snapshot = calculator.calculate(
                1L,
                3L,
                List.of(
                        new ThoughtSimilarity(0.82D, 1D, 0.874D),
                        new ThoughtSimilarity(0.78D, 1D, 0.846D),
                        new ThoughtSimilarity(0.73D, 0.75D, 0.736D)
                )
        );

        assertThat(snapshot.score()).isGreaterThanOrEqualTo(60);
        assertThat(snapshot.visibilityLevel()).isEqualTo(UserVisibilityLevel.REAL_NAME);
    }

    @Test
    void shouldReturnNoneWhenThereAreNoMatches() {
        MatchProperties properties = new MatchProperties();
        UserSimilarityCalculator calculator = new UserSimilarityCalculator(properties);

        UserSimilaritySnapshot snapshot = calculator.calculate(1L, 4L, List.of());

        assertThat(snapshot.score()).isZero();
        assertThat(snapshot.visibilityLevel()).isEqualTo(UserVisibilityLevel.NONE);
    }
}
