package com.xs.bbs.match;

import com.xs.bbs.ai.ThoughtAiProfile;
import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.thought.ThoughtDegree;
import com.xs.bbs.thought.ThoughtPost;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ThoughtSimilarityCalculator {

    private final ThoughtTextAnalyzer thoughtTextAnalyzer;
    private final ThoughtAiService thoughtAiService;

    public ThoughtSimilarityCalculator(ThoughtTextAnalyzer thoughtTextAnalyzer, ThoughtAiService thoughtAiService) {
        this.thoughtTextAnalyzer = thoughtTextAnalyzer;
        this.thoughtAiService = thoughtAiService;
    }

    public ThoughtSimilarity calculate(ThoughtPost left, ThoughtPost right) {
        double lexicalScore = calculateContentScore(left.content(), right.content());
        double semanticScore = calculateSemanticScore(left, right);
        double contentScore = semanticScore < 0D
                ? lexicalScore
                : lexicalScore * 0.3D + semanticScore * 0.7D;
        double degreeScore = calculateDegreeScore(left.degree(), right.degree());
        double finalScore = contentScore * 0.7D + degreeScore * 0.3D;
        return new ThoughtSimilarity(round(contentScore), round(degreeScore), round(finalScore));
    }

    double calculateContentScore(String left, String right) {
        Set<String> leftTokens = thoughtTextAnalyzer.tokens(left);
        Set<String> rightTokens = thoughtTextAnalyzer.tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0D;
        }
        long intersection = leftTokens.stream().filter(rightTokens::contains).count();
        long union = leftTokens.size() + rightTokens.size() - intersection;
        if (union == 0) {
            return 0D;
        }
        return (double) intersection / union;
    }

    double calculateDegreeScore(ThoughtDegree left, ThoughtDegree right) {
        double max = Math.max(left.getWeight(), right.getWeight());
        double min = Math.min(left.getWeight(), right.getWeight());
        return min / max;
    }

    private double calculateSemanticScore(ThoughtPost left, ThoughtPost right) {
        ThoughtAiProfile leftProfile = thoughtAiService.ensureProfile(left);
        ThoughtAiProfile rightProfile = thoughtAiService.ensureProfile(right);
        List<Double> leftEmbedding = leftProfile.embedding();
        List<Double> rightEmbedding = rightProfile.embedding();
        if (leftEmbedding.isEmpty() || rightEmbedding.isEmpty() || leftEmbedding.size() != rightEmbedding.size()) {
            return -1D;
        }
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int index = 0; index < leftEmbedding.size(); index++) {
            double leftValue = leftEmbedding.get(index);
            double rightValue = rightEmbedding.get(index);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }
        if (leftNorm == 0D || rightNorm == 0D) {
            return -1D;
        }
        double cosine = dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
        double normalized = (cosine + 1D) / 2D;
        return Math.max(0D, Math.min(1D, normalized));
    }

    private double round(double value) {
        return Math.round(value * 1000D) / 1000D;
    }
}
