package com.xs.bbs.social;

import com.xs.bbs.match.MatchService;
import com.xs.bbs.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialGraphService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;

    public SocialGraphService(FollowRepository followRepository, UserRepository userRepository, MatchService matchService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.matchService = matchService;
    }

    @Transactional
    public FollowStatusView follow(Long followerId, Long targetId) {
        if (followerId.equals(targetId)) {
            throw new IllegalArgumentException("cannot follow yourself");
        }
        ensureUserExists(followerId);
        ensureUserExists(targetId);
        // 关注不是默认开放能力，必须先经过“实名同频”这一层解锁，和产品关系升级路径保持一致。
        if (!matchService.isRealNameUnlockedBetween(followerId, targetId)) {
            throw new IllegalArgumentException("follow is allowed only after real-name unlock");
        }
        followRepository.saveIfAbsent(followerId, targetId);
        return status(followerId, targetId);
    }

    public FollowStatusView status(Long followerId, Long targetId) {
        boolean followed = followRepository.exists(followerId, targetId);
        boolean mutualFollow = followed && followRepository.exists(targetId, followerId);
        return new FollowStatusView(followerId, targetId, followed, mutualFollow);
    }

    public boolean canViewAllThoughts(Long currentUserId, Long targetUserId) {
        return followRepository.exists(currentUserId, targetUserId) && followRepository.exists(targetUserId, currentUserId);
    }

    private void ensureUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user does not exist"));
    }
}
