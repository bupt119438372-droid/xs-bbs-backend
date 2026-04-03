package com.xs.bbs.match;

import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.common.SearchSupport;
import com.xs.bbs.config.MatchProperties;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchService {

    private final ThoughtRepository thoughtRepository;
    private final UserService userService;
    private final ThoughtSimilarityCalculator thoughtSimilarityCalculator;
    private final UserSimilarityCalculator userSimilarityCalculator;
    private final MatchProperties matchProperties;
    private final ThoughtAiService thoughtAiService;

    public MatchService(
            ThoughtRepository thoughtRepository,
            UserService userService,
            ThoughtSimilarityCalculator thoughtSimilarityCalculator,
            UserSimilarityCalculator userSimilarityCalculator,
            MatchProperties matchProperties,
            ThoughtAiService thoughtAiService
    ) {
        this.thoughtRepository = thoughtRepository;
        this.userService = userService;
        this.thoughtSimilarityCalculator = thoughtSimilarityCalculator;
        this.userSimilarityCalculator = userSimilarityCalculator;
        this.matchProperties = matchProperties;
        this.thoughtAiService = thoughtAiService;
    }

    public List<MatchCandidateView> findCandidatesForThought(ThoughtPost sourceThought) {
        if (!thoughtAiService.allowsMatching(sourceThought)) {
            return List.of();
        }
        List<MatchCandidateView> candidates = new ArrayList<>();
        for (UserProfile targetUser : userService.listUsers()) {
            if (targetUser.id().equals(sourceThought.userId())) {
                continue;
            }
            List<ThoughtSimilarity> matchedThoughts = new ArrayList<>();
            ThoughtPost strongestThought = null;
            double highestThoughtScore = 0D;

            for (ThoughtPost targetThought : thoughtRepository.findByUserId(targetUser.id())) {
                if (!thoughtAiService.allowsMatching(targetThought)) {
                    continue;
                }
                ThoughtSimilarity similarity = thoughtSimilarityCalculator.calculate(sourceThought, targetThought);
                if (similarity.finalScore() < matchProperties.getThoughtThreshold()) {
                    continue;
                }
                matchedThoughts.add(similarity);
                if (similarity.finalScore() > highestThoughtScore) {
                    highestThoughtScore = similarity.finalScore();
                    strongestThought = targetThought;
                }
            }

            UserSimilaritySnapshot snapshot = userSimilarityCalculator.calculate(
                    sourceThought.userId(),
                    targetUser.id(),
                    matchedThoughts
            );
            if (snapshot.visibilityLevel() == UserVisibilityLevel.NONE || strongestThought == null) {
                continue;
            }

            String displayName = snapshot.visibilityLevel() == UserVisibilityLevel.REAL_NAME
                    ? targetUser.nickname()
                    : "匿名用户#" + targetUser.id();
            candidates.add(new MatchCandidateView(
                    targetUser.id(),
                    displayName,
                    strongestThought.content(),
                    highestThoughtScore,
                    snapshot.score(),
                    snapshot.visibilityLevel()
            ));
        }

        candidates.sort(Comparator.comparingInt(MatchCandidateView::userSimilarity).reversed());
        return candidates;
    }

    public List<MatchCandidateView> getUserCandidates(Long userId) {
        return getUserCandidates(userId, null, null);
    }

    public List<MatchCandidateView> getUserCandidates(Long userId, String keyword, UserVisibilityLevel visibilityLevel) {
        return thoughtRepository.findByUserId(userId).stream()
                .filter(thoughtAiService::allowsMatching)
                .findFirst()
                .map(this::findCandidatesForThought)
                .orElseGet(List::of)
                .stream()
                .filter(candidate -> visibilityLevel == null || candidate.visibilityLevel() == visibilityLevel)
                .filter(candidate -> SearchSupport.matches(
                        keyword,
                        candidate.displayName(),
                        candidate.previewThought(),
                        String.valueOf(candidate.userSimilarity()),
                        String.valueOf(candidate.finalScore())
                ))
                .toList();
    }

    public UserVisibilityLevel getVisibilityLevelBetween(Long sourceUserId, Long targetUserId) {
        return getUserCandidates(sourceUserId).stream()
                .filter(candidate -> candidate.targetUserId().equals(targetUserId))
                .map(MatchCandidateView::visibilityLevel)
                .findFirst()
                .orElse(UserVisibilityLevel.NONE);
    }

    public UserVisibilityLevel getMutualVisibilityLevelBetween(Long leftUserId, Long rightUserId) {
        UserVisibilityLevel forward = getVisibilityLevelBetween(leftUserId, rightUserId);
        UserVisibilityLevel backward = getVisibilityLevelBetween(rightUserId, leftUserId);
        // 同频关系对外表现看“双边综合结果”。
        // 这里取两侧中更高的解锁挡位，避免只因为某一侧最近没有触发候选而把已经形成的关系又降回去。
        if (forward == UserVisibilityLevel.REAL_NAME || backward == UserVisibilityLevel.REAL_NAME) {
            return UserVisibilityLevel.REAL_NAME;
        }
        if (forward == UserVisibilityLevel.ANONYMOUS || backward == UserVisibilityLevel.ANONYMOUS) {
            return UserVisibilityLevel.ANONYMOUS;
        }
        return UserVisibilityLevel.NONE;
    }

    public boolean isRealNameUnlockedBetween(Long leftUserId, Long rightUserId) {
        return getMutualVisibilityLevelBetween(leftUserId, rightUserId) == UserVisibilityLevel.REAL_NAME;
    }
}
