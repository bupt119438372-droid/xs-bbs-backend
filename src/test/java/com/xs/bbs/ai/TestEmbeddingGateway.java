package com.xs.bbs.ai;

import com.xs.bbs.match.ThoughtTextAnalyzer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "test")
public class TestEmbeddingGateway implements EmbeddingGateway {

    private static final int DIMENSION = 48;

    private final ThoughtTextAnalyzer thoughtTextAnalyzer;

    public TestEmbeddingGateway(ThoughtTextAnalyzer thoughtTextAnalyzer) {
        this.thoughtTextAnalyzer = thoughtTextAnalyzer;
    }

    @Override
    public EmbeddingGeneration embed(String input) {
        double[] vector = new double[DIMENSION];
        for (String token : thoughtTextAnalyzer.tokens(input)) {
            int bucket = Math.abs(token.hashCode()) % DIMENSION;
            vector[bucket] += 1D;
        }
        double norm = 0D;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> normalized = new ArrayList<>(DIMENSION);
        for (double value : vector) {
            normalized.add(norm == 0D ? 0D : value / norm);
        }
        return new EmbeddingGeneration(List.copyOf(normalized), "test", "test-embedding-v1");
    }
}
