package com.xs.bbs.ai;

import java.util.List;

public record DailyInsightGeneration(
        String headline,
        String interpretation,
        List<String> actions
) {
}
