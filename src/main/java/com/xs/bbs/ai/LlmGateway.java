package com.xs.bbs.ai;

public interface LlmGateway {

    DailyInsightGeneration generateDailyInsight(DailyInsightPrompt prompt);

    ThoughtAnalysisGeneration analyzeThought(ThoughtAnalysisPrompt prompt);
}
