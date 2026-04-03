package com.xs.bbs.common;

import java.util.Locale;

public final class SearchSupport {

    private SearchSupport() {
    }

    public static boolean matches(String keyword, String... values) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalized)) {
                return true;
            }
        }
        return false;
    }
}
