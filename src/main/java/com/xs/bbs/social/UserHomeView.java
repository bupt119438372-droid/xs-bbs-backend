package com.xs.bbs.social;

import com.xs.bbs.match.UserVisibilityLevel;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.user.UserProfile;

import java.util.List;

public record UserHomeView(
        UserProfile profile,
        FollowStatusView followStatus,
        boolean followsYou,
        boolean canViewHome,
        UserVisibilityLevel visibilityLevel,
        boolean fullThoughtAccess,
        int visibleThoughtCount,
        int totalPublicThoughtCount,
        List<ThoughtPost> thoughts
) {
}
