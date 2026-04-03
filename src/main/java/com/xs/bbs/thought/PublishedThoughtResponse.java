package com.xs.bbs.thought;

import com.xs.bbs.ai.ThoughtAiProfileView;
import com.xs.bbs.match.MatchCandidateView;

import java.util.List;

public record PublishedThoughtResponse(
        ThoughtPost thought,
        ThoughtAiProfileView analysis,
        List<MatchCandidateView> candidates
) {
}
