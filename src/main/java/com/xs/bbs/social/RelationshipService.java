package com.xs.bbs.social;

import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.common.SearchSupport;
import com.xs.bbs.match.MatchService;
import com.xs.bbs.match.UserVisibilityLevel;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class RelationshipService {

    private static final int HOME_PREVIEW_LIMIT = 3;

    private final FollowRepository followRepository;
    private final UserService userService;
    private final ThoughtRepository thoughtRepository;
    private final ThoughtAiService thoughtAiService;
    private final MatchService matchService;
    private final SocialGraphService socialGraphService;

    public RelationshipService(
            FollowRepository followRepository,
            UserService userService,
            ThoughtRepository thoughtRepository,
            ThoughtAiService thoughtAiService,
            MatchService matchService,
            SocialGraphService socialGraphService
    ) {
        this.followRepository = followRepository;
        this.userService = userService;
        this.thoughtRepository = thoughtRepository;
        this.thoughtAiService = thoughtAiService;
        this.matchService = matchService;
        this.socialGraphService = socialGraphService;
    }

    public List<SocialUserCardView> listFollowing(Long currentUserId) {
        return listFollowing(currentUserId, null);
    }

    public List<SocialUserCardView> listFollowing(Long currentUserId, String keyword) {
        return followRepository.findTargetIdsByFollowerId(currentUserId).stream()
                .map(targetUserId -> buildUserCard(currentUserId, targetUserId))
                .filter(card -> SearchSupport.matches(keyword, card.nickname(), card.bio(), card.latestThoughtPreview()))
                .toList();
    }

    public List<SocialUserCardView> listFollowers(Long currentUserId) {
        return listFollowers(currentUserId, null);
    }

    public List<SocialUserCardView> listFollowers(Long currentUserId, String keyword) {
        return followRepository.findFollowerIdsByTargetId(currentUserId).stream()
                .map(sourceUserId -> buildUserCard(currentUserId, sourceUserId))
                .filter(card -> SearchSupport.matches(keyword, card.nickname(), card.bio(), card.latestThoughtPreview()))
                .toList();
    }

    public UserHomeView getUserHome(Long currentUserId, Long targetUserId) {
        return getUserHome(currentUserId, targetUserId, null);
    }

    public UserHomeView getUserHome(Long currentUserId, Long targetUserId, String keyword) {
        UserProfile profile = userService.getRequiredUser(targetUserId);
        FollowStatusView followStatus = socialGraphService.status(currentUserId, targetUserId);
        boolean followsYou = followRepository.exists(targetUserId, currentUserId);
        UserVisibilityLevel visibilityLevel = resolveVisibilityLevel(currentUserId, targetUserId);
        boolean canViewHome = canViewHome(currentUserId, targetUserId, followStatus.mutualFollow(), visibilityLevel);
        if (!canViewHome) {
            throw new IllegalArgumentException("you do not have access to this user's home yet");
        }
        // “能进主页”和“能看全量公开念头”是两层权限：
        // 1. 实名同频后可进入主页；
        // 2. 互相关注后才开放全部公开念头，否则只给公开预览。
        boolean fullThoughtAccess = targetUserId.equals(currentUserId) || followStatus.mutualFollow();
        List<ThoughtPost> publicThoughts = thoughtRepository.findByUserId(targetUserId).stream()
                .filter(thoughtAiService::allowsPublicDisplay)
                .toList();
        List<ThoughtPost> visibleThoughts = targetUserId.equals(currentUserId)
                ? thoughtRepository.findByUserId(targetUserId)
                : (fullThoughtAccess
                ? publicThoughts
                : publicThoughts.stream().limit(HOME_PREVIEW_LIMIT).toList());
        List<ThoughtPost> thoughts = visibleThoughts.stream()
                .filter(thought -> SearchSupport.matches(keyword, thought.content()))
                .toList();
        int totalVisibleThoughtCount = targetUserId.equals(currentUserId) ? visibleThoughts.size() : publicThoughts.size();
        return new UserHomeView(
                profile,
                followStatus,
                followsYou,
                true,
                visibilityLevel,
                fullThoughtAccess,
                thoughts.size(),
                totalVisibleThoughtCount,
                thoughts
        );
    }

    public List<ThoughtPost> listFollowingPublicThoughts(Long currentUserId) {
        return listFollowingPublicThoughts(currentUserId, null);
    }

    public List<ThoughtPost> listFollowingPublicThoughts(Long currentUserId, String keyword) {
        return followRepository.findTargetIdsByFollowerId(currentUserId).stream()
                .flatMap(targetUserId -> {
                    UserProfile profile = userService.getRequiredUser(targetUserId);
                    return thoughtRepository.findByUserId(targetUserId).stream()
                            .filter(thought -> SearchSupport.matches(keyword, thought.content(), profile.nickname(), profile.bio()));
                })
                .filter(thoughtAiService::allowsPublicDisplay)
                .sorted(Comparator.comparing(ThoughtPost::createdAt).reversed())
                .toList();
    }

    private SocialUserCardView buildUserCard(Long currentUserId, Long targetUserId) {
        UserProfile profile = userService.getRequiredUser(targetUserId);
        boolean followed = followRepository.exists(currentUserId, targetUserId);
        boolean followsYou = followRepository.exists(targetUserId, currentUserId);
        boolean mutualFollow = followed && followsYou;
        UserVisibilityLevel visibilityLevel = resolveVisibilityLevel(currentUserId, targetUserId);
        List<ThoughtPost> publicThoughts = thoughtRepository.findByUserId(targetUserId).stream()
                .filter(thoughtAiService::allowsPublicDisplay)
                .toList();
        String latestThoughtPreview = publicThoughts.stream()
                .findFirst()
                .map(ThoughtPost::content)
                .orElse("");
        return new SocialUserCardView(
                profile.id(),
                profile.nickname(),
                profile.bio(),
                followed,
                followsYou,
                mutualFollow,
                canViewHome(currentUserId, targetUserId, mutualFollow, visibilityLevel),
                publicThoughts.size(),
                latestThoughtPreview
        );
    }

    private boolean canViewHome(
            Long currentUserId,
            Long targetUserId,
            boolean mutualFollow,
            UserVisibilityLevel visibilityLevel
    ) {
        // 主页访问遵循“本人 always allow；否则实名同频或互相关注后开放”。
        return currentUserId.equals(targetUserId)
                || mutualFollow
                || visibilityLevel == UserVisibilityLevel.REAL_NAME;
    }

    private UserVisibilityLevel resolveVisibilityLevel(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return UserVisibilityLevel.REAL_NAME;
        }
        return matchService.getMutualVisibilityLevelBetween(currentUserId, targetUserId);
    }
}
