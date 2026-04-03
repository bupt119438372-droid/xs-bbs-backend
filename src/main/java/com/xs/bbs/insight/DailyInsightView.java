package com.xs.bbs.insight;

import java.util.List;

public record DailyInsightView(
        Long userId,
        String headline,
        String interpretation,
        List<String> actions
) {
}
