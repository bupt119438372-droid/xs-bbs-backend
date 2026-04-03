package com.xs.bbs.ai;

import java.util.List;

public record EmbeddingGeneration(
        List<Double> vector,
        String provider,
        String model
) {
}
