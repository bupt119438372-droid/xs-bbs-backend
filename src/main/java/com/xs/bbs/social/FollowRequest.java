package com.xs.bbs.social;

import jakarta.validation.constraints.NotNull;

public record FollowRequest(@NotNull Long targetId) {
}
