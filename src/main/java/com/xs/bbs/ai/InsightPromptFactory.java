package com.xs.bbs.ai;

import com.xs.bbs.match.MatchCandidateView;
import com.xs.bbs.match.ThoughtTextAnalyzer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class InsightPromptFactory {

    private final ThoughtTextAnalyzer thoughtTextAnalyzer;

    public InsightPromptFactory(ThoughtTextAnalyzer thoughtTextAnalyzer) {
        this.thoughtTextAnalyzer = thoughtTextAnalyzer;
    }

    public DailyInsightPrompt build(
            Long userId,
            String latestThought,
            ThoughtAiProfile latestAnalysis,
            List<MatchCandidateView> candidates
    ) {
        return new DailyInsightPrompt(
                userId,
                latestThought,
                latestAnalysis.summary(),
                latestAnalysis.tags(),
                detectThemes(latestThought, latestAnalysis.tags()),
                buildResonanceSummary(candidates)
        );
    }

    private List<String> detectThemes(String latestThought, List<String> latestTags) {
        Set<String> themes = new LinkedHashSet<>();
        if (latestTags != null) {
            themes.addAll(latestTags);
        }
        String content = latestThought == null ? "" : latestThought;
        if (content.contains("焦虑") || content.contains("害怕") || content.contains("担心")) {
            themes.add("成长焦虑");
        }
        if (content.contains("方向") || content.contains("长期") || content.contains("人生")) {
            themes.add("长期方向");
        }
        if (content.contains("比较") || content.contains("别人")) {
            themes.add("自我确认");
        }
        if (content.contains("历史") || content.contains("读书")) {
            themes.add("精神积累");
        }

        List<String> analyzerTokens = new ArrayList<>(thoughtTextAnalyzer.tokens(content));
        if (themes.isEmpty() && !analyzerTokens.isEmpty()) {
            themes.add("自我整理");
        }
        return List.copyOf(themes);
    }

    private String buildResonanceSummary(List<MatchCandidateView> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "当前没有强同频对象，适合先继续整理自己的表达。";
        }
        MatchCandidateView top = candidates.getFirst();
        return "目前最同频的是" + top.displayName() + "，相似值为" + top.userSimilarity() + "。";
    }
}
