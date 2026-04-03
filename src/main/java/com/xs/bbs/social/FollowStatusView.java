package com.xs.bbs.social;

public record FollowStatusView(
        Long followerId,
        Long targetId,
        boolean followed,
        boolean mutualFollow
) {
}
