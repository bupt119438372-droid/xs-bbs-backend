package com.xs.bbs.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "test")
public class TestLlmGateway implements LlmGateway {

    @Override
    public DailyInsightGeneration generateDailyInsight(DailyInsightPrompt prompt) {
        String themeSummary = prompt.themes().isEmpty() ? "自我整理" : String.join("、", prompt.themes());
        String anchorTheme = prompt.themes().isEmpty() ? "default" : prompt.themes().getFirst();
        String headline = switch (anchorTheme) {
            case "成长焦虑" -> "今天的念头提醒：焦虑不一定是失控，也可能是你开始认真对待自己的人生。";
            case "长期方向" -> "今天的念头提醒：慢一点没有关系，先看清什么值得你长期投入。";
            case "精神积累" -> "今天的念头提醒：真正留下你的，往往是持续积累而不是即时喧哗。";
            default -> "今天的念头提醒：别急着证明自己，先确认自己真正想留下什么。";
        };
        String interpretation = "系统从你最近的表达里提炼出主题：" + themeSummary + "。"
                + (prompt.latestSummary() == null || prompt.latestSummary().isBlank() ? "" : " 念头摘要是“" + prompt.latestSummary() + "”。")
                + " 你最新的一条念头是“" + trim(prompt.latestThought(), 36) + "”。"
                + " 这说明你关注的不只是眼前情绪，而是在寻找更稳定的人生坐标。"
                + " " + prompt.resonanceSummary();

        List<String> actions = new ArrayList<>();
        actions.add("把今天最重要的一条念头扩写成 100 字短札记");
        if (prompt.themes().contains("长期方向")) {
            actions.add("列出你愿意持续投入 3 个月的一个方向，并写下第一步");
        } else if (prompt.themes().contains("成长焦虑")) {
            actions.add("区分“真正重要的问题”和“别人节奏带来的焦虑”");
        } else {
            actions.add("回看最近 7 天的表达，找出重复出现的主题");
        }
        actions.add("如果愿意，和一位同频用户建立连接");
        return new DailyInsightGeneration(headline, interpretation, List.copyOf(actions));
    }

    @Override
    public ThoughtAnalysisGeneration analyzeThought(ThoughtAnalysisPrompt prompt) {
        String content = prompt.content() == null ? "" : prompt.content().trim();
        Set<String> tags = new LinkedHashSet<>();

        if (content.contains("焦虑") || content.contains("害怕") || content.contains("担心")) {
            tags.add("成长焦虑");
        }
        if (content.contains("方向") || content.contains("长期") || content.contains("人生")) {
            tags.add("长期方向");
        }
        if (content.contains("历史") || content.contains("读书")) {
            tags.add("精神积累");
        }
        if (content.contains("比较") || content.contains("同龄人")) {
            tags.add("自我确认");
        }
        if (tags.isEmpty()) {
            tags.add("自我整理");
        }

        ThoughtModerationStatus status = ThoughtModerationStatus.APPROVED;
        String reason = "内容安全，通过审核。";
        if (containsAny(content, "自杀", "轻生", "杀人", "炸弹")) {
            status = ThoughtModerationStatus.REJECTED;
            reason = "命中高风险内容，禁止进入公开匹配与推荐。";
        } else if (containsAny(content, "赌博", "刷单", "裸聊", "毒品")) {
            status = ThoughtModerationStatus.REVIEW;
            reason = "命中风险词，建议进入人工复核。";
        }

        String summary = content.length() <= 36 ? content : content.substring(0, 36) + "...";
        return new ThoughtAnalysisGeneration(
                summary,
                tags.stream().limit(prompt.maxTags()).toList(),
                status,
                reason,
                "test",
                "test-structured-v1"
        );
    }

    private String trim(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private boolean containsAny(String content, String... keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
