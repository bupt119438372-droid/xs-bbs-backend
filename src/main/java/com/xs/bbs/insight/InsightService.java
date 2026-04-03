package com.xs.bbs.insight;

import com.xs.bbs.ai.DailyInsightGeneration;
import com.xs.bbs.ai.DailyInsightPrompt;
import com.xs.bbs.ai.InsightPromptFactory;
import com.xs.bbs.ai.LlmGateway;
import com.xs.bbs.ai.ThoughtAiProfile;
import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.match.MatchCandidateView;
import com.xs.bbs.match.MatchService;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    private final ThoughtRepository thoughtRepository;
    private final MatchService matchService;
    private final InsightPromptFactory insightPromptFactory;
    private final LlmGateway llmGateway;
    private final ThoughtAiService thoughtAiService;

    public InsightService(
            ThoughtRepository thoughtRepository,
            MatchService matchService,
            InsightPromptFactory insightPromptFactory,
            LlmGateway llmGateway,
            ThoughtAiService thoughtAiService
    ) {
        this.thoughtRepository = thoughtRepository;
        this.matchService = matchService;
        this.insightPromptFactory = insightPromptFactory;
        this.llmGateway = llmGateway;
        this.thoughtAiService = thoughtAiService;
    }

    @Cacheable(cacheNames = "dailyInsight", key = "#userId")
    public DailyInsightView buildDailyInsight(Long userId) {
        ThoughtPost latestThought = thoughtRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("user has no thought yet"));
        ThoughtAiProfile latestAnalysis = thoughtAiService.ensureProfile(latestThought);
        List<MatchCandidateView> candidates = matchService.getUserCandidates(userId);
        DailyInsightPrompt prompt = insightPromptFactory.build(userId, latestThought.content(), latestAnalysis, candidates);
        DailyInsightGeneration generation = generateDailyInsightSafely(userId, latestThought, latestAnalysis, candidates, prompt);
        return new DailyInsightView(
                userId,
                generation.headline(),
                generation.interpretation(),
                generation.actions()
        );
    }

    @CacheEvict(cacheNames = "dailyInsight", key = "#userId")
    public void evictDailyInsight(Long userId) {
    }

    private DailyInsightGeneration generateDailyInsightSafely(
            Long userId,
            ThoughtPost latestThought,
            ThoughtAiProfile latestAnalysis,
            List<MatchCandidateView> candidates,
            DailyInsightPrompt prompt
    ) {
        try {
            return llmGateway.generateDailyInsight(prompt);
        } catch (RuntimeException exception) {
            log.warn("Daily insight degraded for userId={}: {}", userId, exception.getMessage());
            String summary = latestAnalysis.summary() == null || latestAnalysis.summary().isBlank()
                    ? latestThought.content()
                    : latestAnalysis.summary();
            String candidateHint = candidates.isEmpty()
                    ? "今天先把这个念头继续写清楚，等系统恢复后再为你扩展更多共鸣。"
                    : "你已经和 %s 条相近念头产生连接，可以先从最有共鸣的一条继续展开。".formatted(candidates.size());
            return new DailyInsightGeneration(
                    "AI 暂时繁忙，先把重要的念头留住",
                    "你最近反复在意的是：" + summary + "。%s".formatted(candidateHint),
                    List.of(
                            "把这个念头补成三句更具体的话",
                            "记录它和你当下目标的关系",
                            "稍后再回来查看新的 AI 启发"
                    )
            );
        }
    }
}
