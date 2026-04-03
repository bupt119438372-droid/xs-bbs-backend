package com.xs.bbs.match;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class ThoughtTextAnalyzer {

    public Set<String> tokens(String content) {
        String normalized = content == null ? "" : content.trim().toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();
        String[] words = normalized.split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+");
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            tokens.add(word);
            if (containsChinese(word) && word.length() > 1) {
                for (int i = 0; i < word.length() - 1; i++) {
                    tokens.add(word.substring(i, i + 2));
                }
            }
        }
        return tokens;
    }

    private boolean containsChinese(String text) {
        return text.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
