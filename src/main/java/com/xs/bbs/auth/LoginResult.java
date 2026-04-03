package com.xs.bbs.auth;

public record LoginResult(
        String tokenName,
        String tokenValue,
        long timeout,
        SessionUserView user
) {
}
