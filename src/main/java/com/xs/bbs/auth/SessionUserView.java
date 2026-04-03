package com.xs.bbs.auth;

import com.xs.bbs.user.UserProfile;

import java.util.List;

public record SessionUserView(
        Long userId,
        String username,
        UserProfile profile,
        List<String> roles,
        List<String> permissions
) {
}
