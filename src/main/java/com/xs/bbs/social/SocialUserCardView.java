package com.xs.bbs.social;

public record SocialUserCardView(
        Long userId,
        String nickname,
        String bio,
        boolean followed,
        boolean followsYou,
        boolean mutualFollow,
        boolean canViewHome,
        int publicThoughtCount,
        String latestThoughtPreview
) {
}
